"""
Regles d'extraction d'une attestation d'assurance automobile (OCR-DOC)
======================================================================
Module PUR et ISOLE : aucune dependance PaddleOCR / FastAPI — bibliotheque
standard uniquement. Il transforme les lignes brutes d'un OCR en un contrat
JSON FIGE (/lire-attestation) :

    { numeroPolice, numeroQuittance, immatriculation, assure, valideDu,
      valideAu, primeTTC, codeAgence, confiance, statut, texteBrut }

Entree : liste de lignes [{"texte": str, "confiance": float 0..1}, ...]
         (ordre = ordre de lecture haut->bas ; tuples (texte, conf) toleres).
Sortie : dict au contrat ci-dessus. Champ illisible -> None (on n'invente RIEN).

Regles clefs (arbitrages valides) :
- numeroPolice : EXACTEMENT 15 chiffres, ancre sur « Police N° » (fr) et
  « رقم عقد التأمين » (ar). Le numero figure DEUX FOIS sur le document
  (certificat + quittance) : l'extraction exige la CONCORDANCE des lectures.
  Divergence, longueur != 15 ou lecture unique -> statut "a_verifier",
  confiance abaissee.
- numeroQuittance : 8 chiffres apres « N° » (grand numero rouge). C'est AUSSI
  un repere NEGATIF : il n'est JAMAIS renvoye comme numero de police.
- immatriculation algerienne renvoyee COMPACTEE (chiffres seuls, sans espaces).
- dates normalisees JJ/MM/AAAA ; primeTTC en decimale a virgule ("2400,97").

Tous les libelles, regex, fenetres et seuils vivent dans CONFIG ci-dessous,
surchargeables par variables d'environnement (prefixe OCR_) : rien en dur.
"""
import os
import re
import unicodedata

__all__ = ["CONFIG", "extraire_attestation"]


# ---------------------------------------------------------------------------
# CONFIG — surchargeable par env. Lue UNE fois a l'import du module.
# ---------------------------------------------------------------------------

def _env_texte(nom, defaut):
    """Chaine depuis l'env (vide/absente -> defaut)."""
    valeur = os.getenv(nom)
    return valeur if valeur else defaut


def _env_liste(nom, defaut):
    """Liste CSV depuis l'env (vide/absente -> defaut)."""
    brut = os.getenv(nom)
    if not brut:
        return list(defaut)
    return [p.strip() for p in brut.split(",") if p.strip()]


def _env_nombre(nom, defaut):
    try:
        return float(os.getenv(nom, ""))
    except ValueError:
        return defaut


def _env_entier(nom, defaut):
    try:
        return int(os.getenv(nom, ""))
    except ValueError:
        return defaut


CONFIG = {
    # --- Numero de POLICE : 15 chiffres exactement, present DEUX fois (certificat + quittance)
    "ANCRES_POLICE": _env_liste("OCR_ANCRES_POLICE", ["police n", "رقم عقد التأمين"]),
    "LONGUEUR_POLICE": _env_entier("OCR_LONGUEUR_POLICE", 15),
    "FENETRE_POLICE": _env_entier("OCR_FENETRE_POLICE", 2),  # lignes suivantes fouillees si l'ancre est seule
    # --- Numero de QUITTANCE : 8 chiffres apres « N° » (grand numero rouge) — repere NEGATIF pour la police
    "REGEX_QUITTANCE": _env_texte("OCR_REGEX_QUITTANCE", r"\bn[°ºo]?\s*[:.]?\s*(?<!\d)(\d{8})(?!\d)"),
    "LONGUEUR_QUITTANCE": _env_entier("OCR_LONGUEUR_QUITTANCE", 8),
    # --- Immatriculation algerienne : NNNNN(N) NNN NN — les gardes (?<!\d)/(?!\d) empechent
    #     de matcher un morceau d'un numero plus long (ex. les 11 premiers chiffres de la police)
    "REGEX_IMMAT": _env_texte("OCR_REGEX_IMMAT", r"(?<!\d)(\d{5,6}[ ]?\d{3}[ ]?\d{2})(?!\d)"),
    # --- Code agence : NN.AA.NNNN
    "REGEX_AGENCE": _env_texte("OCR_REGEX_AGENCE", r"(?<!\d)(\d{2}\.[A-Z]{2}\.\d{4})(?!\d)"),
    # --- Periode de validite : « Effet du ... Au ... » / « صالحة من ... إلى »
    "ANCRES_VALIDITE": _env_liste("OCR_ANCRES_VALIDITE", ["effet du", "valable du", "صالحة من"]),
    "REGEX_DATE": _env_texte("OCR_REGEX_DATE", r"(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{4})"),
    "FENETRE_DATES": _env_entier("OCR_FENETRE_DATES", 2),
    # --- Prime TTC : montant a virgule PROCHE d'un libelle (ordre de la liste = priorite, TTC d'abord)
    "ANCRES_PRIME": _env_liste("OCR_ANCRES_PRIME", ["ttc", "prime"]),
    "REGEX_MONTANT": _env_texte("OCR_REGEX_MONTANT", r"(?<!\d)(\d{1,3}(?:[ . ]\d{3})+|\d+),(\d{2})(?!\d)"),
    "FENETRE_PRIME": _env_entier("OCR_FENETRE_PRIME", 1),
    # --- Assure (nom) : libelle fr/ar
    "ANCRES_ASSURE": _env_liste("OCR_ANCRES_ASSURE", ["assure", "المؤمن له"]),
    "LONGUEUR_MAX_ASSURE": _env_entier("OCR_LONGUEUR_MAX_ASSURE", 80),
    # --- Seuils de confiance
    "SEUIL_CONFIANCE": _env_nombre("OCR_SEUIL_CONFIANCE", 0.60),      # sous ce niveau -> "a_verifier"
    "CONFIANCE_MOYENNE": _env_nombre("OCR_CONFIANCE_MOYENNE", 0.50),  # plafond si concordance NON verifiee
    "CONFIANCE_BASSE": _env_nombre("OCR_CONFIANCE_BASSE", 0.25),      # plafond si divergence / hors format
}


# ---------------------------------------------------------------------------
# Normalisation + compilation des motifs (une fois, depuis la CONFIG)
# ---------------------------------------------------------------------------

def _normaliser(texte):
    """Minuscules + suppression des accents SANS changer la longueur : les index
    restent alignes avec le texte brut (indispensable pour decouper apres une
    ancre). Un caractere entre -> un caractere sort (é -> e, أ -> ا, ...)."""
    sortie = []
    for car in texte or "":
        decompose = unicodedata.normalize("NFD", car)
        sortie.append((decompose[0] if decompose else car).lower())
    return "".join(sortie)


def _compiler_ancre(ancre, borne_fin=False):
    """Ancre (libelle) -> regex appliquee au texte NORMALISE.
    Libelle latin : bornes de mot + espaces souples (« police n » matche
    « Police N° », « POLICE  No », ...). Libelle arabe : recherche directe
    (\\b n'est pas fiable en arabe), espaces souples aussi."""
    norme = _normaliser(ancre)
    corps = re.escape(norme).replace("\\ ", " ").replace(" ", r"\s+")
    if re.search(r"[a-z]", norme):
        return re.compile(r"\b" + corps + (r"\b" if borne_fin else ""))
    return re.compile(corps)


RE_QUITTANCE = re.compile(CONFIG["REGEX_QUITTANCE"], re.IGNORECASE)
RE_IMMAT = re.compile(CONFIG["REGEX_IMMAT"])
RE_AGENCE = re.compile(CONFIG["REGEX_AGENCE"])
RE_DATE = re.compile(CONFIG["REGEX_DATE"])
RE_MONTANT = re.compile(CONFIG["REGEX_MONTANT"])
RE_ANCRES_POLICE = [_compiler_ancre(a) for a in CONFIG["ANCRES_POLICE"]]
RE_ANCRES_VALIDITE = [_compiler_ancre(a, borne_fin=True) for a in CONFIG["ANCRES_VALIDITE"]]
RE_ANCRES_PRIME = [_compiler_ancre(a, borne_fin=True) for a in CONFIG["ANCRES_PRIME"]]
RE_ANCRES_ASSURE = [_compiler_ancre(a, borne_fin=True) for a in CONFIG["ANCRES_ASSURE"]]


# ---------------------------------------------------------------------------
# Petites briques
# ---------------------------------------------------------------------------

def _preparer_lignes(lignes):
    """Tolere dicts {"texte","confiance"} et tuples (texte, conf). Ecarte les
    lignes vides, borne la confiance dans [0..1]."""
    paires = []
    for ligne in lignes or []:
        if isinstance(ligne, dict):
            texte = str(ligne.get("texte") or "")
            conf = ligne.get("confiance", 0.0)
        else:
            texte = str(ligne[0]) if len(ligne) > 0 else ""
            conf = ligne[1] if len(ligne) > 1 else 0.0
        texte = texte.strip()
        if not texte:
            continue
        try:
            conf = float(conf)
        except (TypeError, ValueError):
            conf = 0.0
        paires.append((texte, max(0.0, min(1.0, conf))))
    return paires


def _a_ancre_police(ligne_normee):
    return any(a.search(ligne_normee) for a in RE_ANCRES_POLICE)


def _candidat_chiffres(texte):
    """Concatene les groupes de chiffres d'un segment : l'OCR coupe parfois
    « 123 456 789 012 345 » en plusieurs blocs."""
    return "".join(re.findall(r"\d+", texte or ""))


# ---------------------------------------------------------------------------
# Extracteurs de champs (chacun renvoie (valeur, index_ligne) ou (None, None))
# ---------------------------------------------------------------------------

def _extraire_quittance(textes, normes):
    """8 chiffres apres « N° ». JAMAIS sur une ligne portant une ancre de
    police (le N° qui y figure est le numero de POLICE, pas la quittance)."""
    for i, brut in enumerate(textes):
        if _a_ancre_police(normes[i]):
            continue
        m = RE_QUITTANCE.search(brut)
        if m:
            return m.group(1), i
    return None, None


def _collecter_occurrences_police(textes, normes, quittance):
    """Toutes les lectures du numero de police, une par ancre trouvee.
    Le numero est cherche apres l'ancre, avant l'ancre (cas arabe, RTL), puis
    dans les FENETRE_POLICE lignes suivantes. Le grand numero rouge (8 chiffres
    / egal a la quittance) est un repere NEGATIF : jamais retenu comme police."""
    occurrences = []  # [(index_ligne, chiffres), ...]
    for i, ligne in enumerate(normes):
        m = None
        for ancre in RE_ANCRES_POLICE:
            m = ancre.search(ligne)
            if m:
                break
        if not m:
            continue
        brut = textes[i]
        cand = _candidat_chiffres(brut[m.end():]) or _candidat_chiffres(brut[:m.start()])
        if not cand:
            # L'ancre est seule sur sa ligne : le numero est en dessous.
            for j in range(i + 1, min(i + 1 + CONFIG["FENETRE_POLICE"], len(textes))):
                if _a_ancre_police(normes[j]):
                    break  # nouvelle zone ancree : elle produira sa propre occurrence
                c = _candidat_chiffres(textes[j])
                if not c:
                    continue
                if len(c) == CONFIG["LONGUEUR_QUITTANCE"] or (quittance and c == quittance):
                    continue  # repere NEGATIF : c'est la quittance, pas la police
                cand = c
                break
        if not cand:
            continue
        if len(cand) == CONFIG["LONGUEUR_QUITTANCE"] or (quittance and cand == quittance):
            continue  # ne JAMAIS renvoyer le numero de quittance comme police
        occurrences.append((i, cand))
    return occurrences


def _arbitrer_police(candidats):
    """Applique l'EXIGENCE DE CONCORDANCE. Renvoie (numero|None, ok, degre) :
    - ok=True  : >= 2 lectures identiques de 15 chiffres, aucune lecture parasite ;
    - degre "moyen" : une seule lecture exploitable (concordance non verifiable) ;
    - degre "bas"   : divergence ou longueur != 15.
    MODE ASSISTE : des qu'au moins une lecture existe, le numero est RENVOYE (l'agent
    le corrige a l'ecran) — mais ok/degre plafonnent la confiance : on n'affirme rien."""
    valides = [c for c in candidats if len(c) == CONFIG["LONGUEUR_POLICE"]]
    anomalies = [c for c in candidats if len(c) != CONFIG["LONGUEUR_POLICE"]]
    uniques = set(valides)
    if len(uniques) == 1 and len(valides) >= 2 and not anomalies:
        return valides[0], True, "ok"
    if len(uniques) > 1:
        return valides[0], False, "bas"    # lectures divergentes : renvoyee pour correction, jamais affirmee
    if len(uniques) == 1:
        # Une seule lecture de 15 chiffres : on la garde mais concordance non garantie.
        return valides[0], False, ("moyen" if not anomalies else "bas")
    if anomalies:
        return anomalies[0], False, "bas"  # hors format (ex. 14 chiffres) : renvoyee a corriger
    return None, False, "bas"              # introuvable


def _extraire_immatriculation(textes):
    for i, brut in enumerate(textes):
        m = RE_IMMAT.search(brut)
        if m:
            return re.sub(r"\s", "", m.group(1)), i  # compactee : chiffres seuls
    return None, None


def _extraire_code_agence(textes):
    for i, brut in enumerate(textes):
        m = RE_AGENCE.search(brut)
        if m:
            return m.group(1), i
    return None, None


def _formater_date(triplet):
    jour, mois, annee = triplet
    return "%02d/%02d/%s" % (int(jour), int(mois), annee)


def _extraire_validite(textes, normes):
    """« Effet du JJ/MM/AAAA Au JJ/MM/AAAA » (ou libelle arabe). Les deux dates
    peuvent deborder sur les lignes suivantes (fenetre FENETRE_DATES)."""
    for i, ligne in enumerate(normes):
        if not any(a.search(ligne) for a in RE_ANCRES_VALIDITE):
            continue
        dates = []
        for j in range(i, min(i + 1 + CONFIG["FENETRE_DATES"], len(textes))):
            dates.extend(RE_DATE.findall(textes[j]))
            if len(dates) >= 2:
                break
        du = _formater_date(dates[0]) if dates else None
        au = _formater_date(dates[1]) if len(dates) >= 2 else None
        if du or au:
            return du, au, i
    return None, None, None


def _extraire_prime(textes, normes):
    """Montant decimal a virgule proche d'un libelle TTC/Prime. Normalise sans
    separateurs de milliers : « 2 400,97 » -> « 2400,97 »."""
    for ancre in RE_ANCRES_PRIME:  # ordre CONFIG = priorite (TTC avant Prime)
        for i, ligne in enumerate(normes):
            if not ancre.search(ligne):
                continue
            for j in range(i, min(i + 1 + CONFIG["FENETRE_PRIME"], len(textes))):
                montants = RE_MONTANT.findall(textes[j])
                if montants:
                    entier, centimes = montants[-1]  # le montant suit le libelle
                    return re.sub(r"[ . ]", "", entier) + "," + centimes, j
    return None, None


def _extraire_assure(textes, normes):
    """Nom de l'assure apres le libelle « Assuré » / « المؤمن له » (meme ligne,
    sinon ligne suivante)."""
    for i, ligne in enumerate(normes):
        for ancre in RE_ANCRES_ASSURE:
            m = ancre.search(ligne)
            if not m:
                continue
            valeur = textes[i][m.end():]
            # Nettoie les restes de libelle : « (e) », separateurs, espaces.
            valeur = re.sub(r"^\s*(\(e\))?\s*[:.\-–—|]*\s*", "", valeur, flags=re.IGNORECASE).strip()
            if not valeur and i + 1 < len(textes):
                valeur = textes[i + 1].strip()
            if valeur:
                return valeur[:CONFIG["LONGUEUR_MAX_ASSURE"]].strip(), i
    return None, None


# ---------------------------------------------------------------------------
# Point d'entree
# ---------------------------------------------------------------------------

def extraire_attestation(lignes):
    """Lignes OCR -> contrat JSON /lire-attestation (voir docstring du module)."""
    paires = _preparer_lignes(lignes)
    textes = [t for t, _ in paires]
    confs = [c for _, c in paires]
    normes = [_normaliser(t) for t in textes]
    texte_brut = "\n".join(textes)
    utiles = set()  # index des lignes ayant fourni au moins un champ

    quittance, idx = _extraire_quittance(textes, normes)
    if idx is not None:
        utiles.add(idx)

    occurrences = _collecter_occurrences_police(textes, normes, quittance)
    utiles.update(i for i, _ in occurrences)
    police, police_ok, degre = _arbitrer_police([c for _, c in occurrences])

    immatriculation, idx = _extraire_immatriculation(textes)
    if idx is not None:
        utiles.add(idx)
    code_agence, idx = _extraire_code_agence(textes)
    if idx is not None:
        utiles.add(idx)
    valide_du, valide_au, idx = _extraire_validite(textes, normes)
    if idx is not None:
        utiles.add(idx)
    prime_ttc, idx = _extraire_prime(textes, normes)
    if idx is not None:
        utiles.add(idx)
    assure, idx = _extraire_assure(textes, normes)
    if idx is not None:
        utiles.add(idx)

    # Confiance de base = moyenne des lignes UTILES (repli : toutes les lignes).
    if utiles:
        conf_base = sum(confs[i] for i in utiles) / len(utiles)
    elif confs:
        conf_base = sum(confs) / len(confs)
    else:
        conf_base = 0.0

    if police_ok and conf_base >= CONFIG["SEUIL_CONFIANCE"]:
        statut, confiance = "lu", conf_base
    elif police_ok:
        statut, confiance = "a_verifier", conf_base  # police concordante mais OCR douteux
    else:
        plafond = CONFIG["CONFIANCE_MOYENNE"] if degre == "moyen" else CONFIG["CONFIANCE_BASSE"]
        statut, confiance = "a_verifier", min(conf_base, plafond)

    return {
        "numeroPolice": police,
        "numeroQuittance": quittance,
        "immatriculation": immatriculation,
        "assure": assure,
        "valideDu": valide_du,
        "valideAu": valide_au,
        "primeTTC": prime_ttc,
        "codeAgence": code_agence,
        "confiance": round(max(0.0, min(1.0, confiance)), 3),
        "statut": statut,
        "texteBrut": texte_brut,
    }
