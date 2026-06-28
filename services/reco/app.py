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

# Classes COCO considerees comme "vehicule"
COCO_VEHICULE = {2: "voiture", 3: "moto", 5: "bus", 7: "camion"}

app = FastAPI(title="Service RECO — INTRAGAM", version="1.0")

_yolo = None  # modele detection vehicule
_alpr = None  # pipeline plaque


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


def _lire_plaque(arr: np.ndarray):
    """Retourne (plaque_normalisee | None, confiance) via fast-alpr."""
    results = _alpr.predict(arr)
    meilleur = None  # (texte, conf)
    for r in results:
        ocr = getattr(r, "ocr", None)
        txt = getattr(ocr, "text", None) if ocr else None
        conf = getattr(ocr, "confidence", None) if ocr else None
        if conf is None:  # repli sur la confiance du detecteur
            det = getattr(r, "detection", None)
            conf = getattr(det, "confidence", 0.0) if det else 0.0
        if txt and (meilleur is None or float(conf) > meilleur[1]):
            meilleur = (txt, float(conf))
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
        "plaque": plaque,                       # None si non lue / vue sans plaque
        "confiance": round(conf_plaque, 3),     # confiance de la lecture de plaque
        "confianceVehicule": round(conf_veh, 3),
        "vue": vue,
    })
