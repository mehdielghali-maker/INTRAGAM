"""
Service RECO — Reconnaissance de vehicule + lecture de plaque (ANPR/LAPI)
========================================================================
Solution INDEPENDANTE et auto-hebergee pour INTRAGAM (aucun couplage avec
PROASSUR, GAMIA ou un autre service interne). On peut la remplacer par un
autre moteur sans toucher au reste : INTRAGAM ne parle qu'au port "reconnaissance".

Pipeline :
  1) Detection "est-ce un vehicule ?"  -> YOLO (ultralytics, classes COCO
     voiture / moto / bus / camion).
  2) Lecture de la plaque              -> fast-alpr (detecteur de plaque + OCR ONNX).

Entree  : image (multipart "photo") + "vue" optionnelle
          (avant / arriere / gauche / droite / toit).
Sortie  : JSON { estVehicule, typeVehicule, plaque, confiance,
                 confianceVehicule, vue }.

IMPORTANT : la COMPARAISON plaque <-> immatriculation du CONTRAT se fait dans
INTRAGAM (qui connait le contrat), PAS ici. Ce service lit une plaque, il ne
juge pas la conformite.
"""
import io
import os
import re
import logging
from typing import Optional

import numpy as np
from PIL import Image
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("reco")

# ----------------------------------------------------------------------------
# Configuration (tout par variables d'environnement — rien en dur)
# ----------------------------------------------------------------------------
SEUIL_VEHICULE = float(os.getenv("RECO_SEUIL_VEHICULE", "0.45"))  # conf. mini "vehicule"
SEUIL_PLAQUE = float(os.getenv("RECO_SEUIL_PLAQUE", "0.35"))      # conf. mini lecture plaque
YOLO_MODEL = os.getenv("RECO_YOLO_MODEL", "yolov8n.pt")           # nano = rapide CPU
DET_MODEL = os.getenv("RECO_DET_MODEL", "yolo-v9-t-384-license-plate-end2end")
OCR_MODEL = os.getenv("RECO_OCR_MODEL", "cct-xs-v2-global-model")
# Plaques purement NUMERIQUES (Algerie) : masque les lettres dans la sortie OCR -> l'argmax ne peut
# plus sortir une lettre (ex. "L" lu au lieu de "4"). true par defaut (cible DZ). NB : ne change PAS
# la capacite du modele (toujours <= max_plate_slots) ; corrige seulement les confusions lettre/chiffre.
PLAQUE_NUMERIQUE = os.getenv("RECO_PLAQUE_NUMERIQUE", "true").lower() in ("1", "true", "yes", "on")
# Longueur cible de la plaque (Algerie = 11) : les chiffres NON LUS (modele tronque a max_plate_slots)
# sont completes par des "*" pour atteindre cette longueur. 0 = desactive. Le "*" est IGNORE par la
# comparaison plaque<->contrat (normalisation cote poste : seuls [A-Z0-9] comptent).
PLAQUE_LONGUEUR = int(os.getenv("RECO_PLAQUE_LONGUEUR", "0") or "0")

# Classes COCO considerees comme "vehicule"
COCO_VEHICULE = {2: "voiture", 3: "moto", 5: "bus", 7: "camion"}

app = FastAPI(title="Service RECO — INTRAGAM", version="1.0")

# CORS : les fronts (PWA declaration, poste) appellent /analyser depuis le NAVIGATEUR. Origines
# autorisees par variable d'environnement (rien en dur) ; "*" en dev.
_cors_origins = [o.strip() for o in os.getenv("RECO_CORS_ORIGINS", "*").split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_origins or ["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_yolo = None  # modele detection vehicule
_alpr = None  # pipeline plaque


def _forcer_sortie_numerique(alpr) -> None:
    """Force la lecture OCR a ne produire que des CHIFFRES (plaques DZ numeriques).

    On enrobe la session ONNX du modele OCR pour ANNULER les probabilites des classes non-chiffres
    dans la sortie "plaque" : l'argmax (fait ensuite par fast-plate-ocr) ne peut donc choisir qu'un
    chiffre (ou le pad). Aucune copie/reentrainement : seules les confusions lettre/chiffre (ex. "L"
    pour "4", "O" pour "0") sont eliminees. La limite des slots (longueur max) reste inchangee.
    """
    rec = alpr.ocr.ocr_model
    alphabet = rec.config.alphabet
    autorise = set("0123456789" + (rec.config.pad_char or ""))
    masque = np.array([1.0 if c in autorise else 0.0 for c in alphabet], dtype=np.float32)
    nom_plaque = rec.plate_output_name
    run_origine = rec.model.run

    def run_masque(output_names, input_feed, *args, **kwargs):
        sorties = run_origine(output_names, input_feed, *args, **kwargs)
        noms = output_names if output_names else rec.output_names
        return [s * masque if n == nom_plaque else s for n, s in zip(noms, sorties)]

    rec.model.run = run_masque


@app.on_event("startup")
def _charger_modeles() -> None:
    """Charge les modeles UNE fois au demarrage (et non a chaque requete)."""
    global _yolo, _alpr
    from ultralytics import YOLO
    from fast_alpr import ALPR

    log.info("Chargement YOLO (%s)...", YOLO_MODEL)
    _yolo = YOLO(YOLO_MODEL)
    log.info("Chargement ALPR (det=%s, ocr=%s)...", DET_MODEL, OCR_MODEL)
    _alpr = ALPR(detector_model=DET_MODEL, ocr_model=OCR_MODEL)
    if PLAQUE_NUMERIQUE:
        _forcer_sortie_numerique(_alpr)
        log.info("OCR contraint aux CHIFFRES (plaques numeriques, ex. Algerie).")
    log.info("Modeles charges.")


def _vers_ndarray(data: bytes) -> np.ndarray:
    """Bytes -> image BGR (convention OpenCV/YOLO)."""
    try:
        img = Image.open(io.BytesIO(data)).convert("RGB")
    except Exception as exc:  # image corrompue / format non gere
        raise HTTPException(status_code=400, detail=f"Image illisible: {exc}")
    return np.array(img)[:, :, ::-1]  # RGB -> BGR


def _detecter_vehicule(arr: np.ndarray):
    """Retourne (est_vehicule, type, confiance) via YOLO."""
    res = _yolo.predict(arr, verbose=False)[0]
    meilleur_conf = 0.0
    meilleur_type = None
    for box in res.boxes:
        cls = int(box.cls[0])
        conf = float(box.conf[0])
        if cls in COCO_VEHICULE and conf > meilleur_conf:
            meilleur_conf = conf
            meilleur_type = COCO_VEHICULE[cls]
    return (meilleur_conf >= SEUIL_VEHICULE), meilleur_type, meilleur_conf


def _normaliser_plaque(txt: str) -> str:
    """Plaques algeriennes = chiffres. On garde l'alphanumerique en majuscules."""
    return re.sub(r"[^A-Z0-9]", "", (txt or "").upper())


def _completer_plaque(plaque):
    """Complete les chiffres NON LUS par des '*' jusqu'a PLAQUE_LONGUEUR (ex. 11 pour DZ).

    Le modele lit un prefixe puis s'arrete (cap a max_plate_slots) : on marque visuellement les
    positions manquantes. Le '*' est ignore par la comparaison plaque<->contrat cote poste.
    """
    if plaque and PLAQUE_LONGUEUR and len(plaque) < PLAQUE_LONGUEUR:
        return plaque + "*" * (PLAQUE_LONGUEUR - len(plaque))
    return plaque


def _coerce_conf(c) -> Optional[float]:
    """Confiance robuste. Selon la version de fast-alpr, `confidence` peut etre un float OU une
    LISTE de confiances par caractere (auquel cas on prend la moyenne). None si inexploitable."""
    if c is None:
        return None
    if isinstance(c, (list, tuple)):
        vals = [float(x) for x in c if isinstance(x, (int, float))]
        return sum(vals) / len(vals) if vals else None
    try:
        return float(c)
    except (TypeError, ValueError):
        return None


def _lire_plaque(arr: np.ndarray):
    """Retourne (plaque_normalisee | None, confiance) via fast-alpr."""
    results = _alpr.predict(arr)
    meilleur = None  # (texte, conf)
    for r in results:
        ocr = getattr(r, "ocr", None)
        txt = getattr(ocr, "text", None) if ocr else None
        conf = _coerce_conf(getattr(ocr, "confidence", None) if ocr else None)
        if conf is None:  # repli sur la confiance du detecteur
            det = getattr(r, "detection", None)
            conf = _coerce_conf(getattr(det, "confidence", None) if det else None) or 0.0
        if txt and (meilleur is None or conf > meilleur[1]):
            meilleur = (txt, conf)
    if not meilleur:
        return None, 0.0
    return _normaliser_plaque(meilleur[0]), meilleur[1]


@app.get("/health")
def health():
    """Sonde de sante (readiness)."""
    return {"status": "ok", "modeles_charges": _yolo is not None and _alpr is not None}


@app.post("/analyser")
async def analyser(photo: UploadFile = File(...), vue: Optional[str] = Form(None)):
    """
    Analyse une photo de vehicule.
    - "vue" guide le traitement : la plaque n'est cherchee que sur avant/arriere
      (ou si la vue n'est pas precisee). Sur gauche/droite/toit : detection seule.
    """
    data = await photo.read()
    if not data:
        raise HTTPException(status_code=400, detail="Fichier vide")
    arr = _vers_ndarray(data)

    est_vehicule, type_vehicule, conf_veh = _detecter_vehicule(arr)

    plaque, conf_plaque = None, 0.0
    if vue in (None, "avant", "arriere"):
        plaque, conf_plaque = _lire_plaque(arr)
        if conf_plaque < SEUIL_PLAQUE:
            plaque = None  # trop incertain : on prefere ne rien affirmer

    return JSONResponse({
        "estVehicule": bool(est_vehicule),
        "typeVehicule": type_vehicule,
        "plaque": _completer_plaque(plaque),    # None si non lue ; sinon complete par "*" jusqu'a 11
        "confiance": round(conf_plaque, 3),     # confiance de la lecture de plaque
        "confianceVehicule": round(conf_veh, 3),
        "vue": vue,
    })
