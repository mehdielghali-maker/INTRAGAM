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
Sortie : dict au contrat ci-dessus. Champ illisible -> None (on n'invente RIEN ;
         seule exception : un numero de police HORS FORMAT est renvoye pour
         CORRECTION en mode assiste, avec confiance plafonnee basse).

Regles clefs (calees sur le DOCUMENT REEL et la sortie OCR REELLE) :
- numeroPolice : EXACTEMENT 15 chiffres, present PLUSIEURS fois (certificat +
  quittance « en face d'Assuré »). CONCORDANCE PAR MAJORITE : >= 2 lectures
  identiques de 15 chiffres valent concordance (les lectures parasites d'un
  OCR bruite n'annulent pas un accord net) ; sinon statut "a_verifier".
- numeroQuittance : 8 chiffres apres « N° » (grand numero rouge). C'est AUSSI
  un repere NEGATIF : il n'est JAMAIS renvoye comme numero de police. L'OCR
  separe parfois le « N° » du numero -> repli : bloc de 8 chiffres SEUL sur sa
  ligne, adjacent a une ligne « N »/« N° »/« quittance ».
- L'OCR COLLE les libelles (« PoliceN: », « Effetdu: », « PrimeTTC: ») -> les
  espaces des ancres sont OPTIONNELS. L'arabe ressort souvent INVERSE
  (« نيمأتلا دقع مقر ») -> chaque ancre est aussi testee sur la ligne RETOURNEE.
- immatriculation algerienne : separateurs OBLIGATOIRES entre les groupes
  (un bloc de chiffres contigus matcherait n'importe quel numero administratif).

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
    # --- Numero de POLICE : 15 chiffres exactement, present PLUSIEURS fois sur le document
    "ANCRES_POLICE": _env_liste("OCR_ANCRES_POLICE", ["police n", "رقم عقد التأمين"]),
    "LONGUEUR_POLICE": _env_entier("OCR_LONGUEUR_POLICE", 15),
    "FENETRE_POLICE": _env_entier("OCR_FENETRE_POLICE", 2),  # lignes fouillees autour d'une ancre seule
    # En fenetre, un candidat doit etre un GRAND bloc de chiffres : ecarte les lignes de dates,
    # montants ou petits numeros voisins (« Police N° : » vide suivi d'« Effet du 04/05/2026 »...).
    "LONGUEUR_MIN_FENETRE": _env_entier("OCR_LONGUEUR_MIN_FENETRE", 10),
    # Candidats STRUCTURELS : une ligne qui n'est QU'un grand bloc de chiffres proche de la
    # longueur d'une police est une lecture de police meme sans ancre lisible (l'OCR corrompt
    # souvent les libelles). Bornes en nombre de chiffres autour de LONGUEUR_POLICE.
    "TOLERANCE_STRUCTURELLE": _env_entier("OCR_TOLERANCE_STRUCTURELLE", 2),
    # --- Numero de QUITTANCE : 8 chiffres apres « N° » (grand numero rouge) — repere NEGATIF.
    #     Variantes de glyphes OCR tolerees (N°, No, N⁰) ; « № » est UN caractere a part entiere.
    "REGEX_QUITTANCE": _env_texte("OCR_REGEX_QUITTANCE", r"(?:\bn[°ºo0⁰]?|№)\s*[:.]?\s*(?<!\d)(\d{8})(?!\d)"),
    "LONGUEUR_QUITTANCE": _env_entier("OCR_LONGUEUR_QUITTANCE", 8),
    # Repli quand l'OCR separe le « N° » du numero : motif d'une ligne-libelle « N »/« N° »/« NO ».
    "REGEX_LIGNE_N": _env_texte("OCR_REGEX_LIGNE_N", r"^\s*(?:n[o0°º⁰]{0,2}|№)\s*[:.]?\s*$"),
    "FENETRE_QUITTANCE": _env_entier("OCR_FENETRE_QUITTANCE", 2),
    # --- Immatriculation algerienne : NNNN(NN) NNN NN — separateurs OBLIGATOIRES entre groupes
    #     (1er groupe 4 a 6 chiffres : les camions/anciens formats portent « 0666 309 40 »)
    "REGEX_IMMAT": _env_texte("OCR_REGEX_IMMAT", r"(?<!\d)(\d{4,6}[ .\-]\d{3}[ .\-]\d{2})(?!\d)"),
    # --- Code agence : NN.AA.NNNN — espaces autour des points toleres (tampon « 40. AR.0105 »)
    "REGEX_AGENCE": _env_texte("OCR_REGEX_AGENCE", r"(?<!\d)(\d{2}\s*\.\s*[A-Z]{2}\s*\.\s*\d{4})(?!\d)"),
    # --- Periode de validite : « Effet du ... Au ... » / « صالحة من ... إلى »
    "ANCRES_VALIDITE": _env_liste("OCR_ANCRES_VALIDITE", ["effet du", "valable du", "صالحة من"]),
    "REGEX_DATE": _env_texte("OCR_REGEX_DATE", r"(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{4})"),
    "FENETRE_DATES": _env_entier("OCR_FENETRE_DATES", 2),
    # --- Prime TTC : montant a virgule PROCHE d'un libelle (ordre de la liste = priorite, TTC d'abord).
    #     Pas de garde finale : l'OCR colle des suites (« 2400,97DA » lu « 2400,970A »).
    "ANCRES_PRIME": _env_liste("OCR_ANCRES_PRIME", ["ttc", "prime"]),
    "REGEX_MONTANT": _env_texte("OCR_REGEX_MONTANT", r"(?<!\d)(\d{1,3}(?:[ . ]\d{3})+|\d+),(\d{2})"),
    "FENETRE_PRIME": _env_entier("OCR_FENETRE_PRIME", 1),
    # --- Assure (nom) : libelles fr/ar (« السيد (ة) » = « M./Mme » sur la vignette certificat)
    "ANCRES_ASSURE": _env_liste("OCR_ANCRES_ASSURE", ["assure", "المؤمن له", "السيد"]),
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
    """Ancre (libelle) -> regex appliquee au texte NORMALISE. Les ESPACES du libelle
    deviennent OPTIONNELS (\\s*) : l'OCR colle souvent les mots (« PoliceN: »,
    « PrimeTTC: »). Libelle latin : borne de mot au debut. Libelle arabe : recherche
    directe (\\b n'est pas fiable en arabe)."""
    norme = _normaliser(ancre)
    corps = re.escape(norme).replace("\\ ", " ").replace(" ", r"\s*")
    if re.search(r"[a-z]", norme):
        return re.compile(r"\b" + corps + (r"\b" if borne_fin else ""))
    return re.compile(corps)


RE_QUITTANCE = re.compile(CONFIG["REGEX_QUITTANCE"], re.IGNORECASE)
RE_LIGNE_N = re.compile(CONFIG["REGEX_LIGNE_N"], re.IGNORECASE)
RE_IMMAT = re.compile(CONFIG["REGEX_IMMAT"])
RE_AGENCE = re.compile(CONFIG["REGEX_AGENCE"])
RE_DATE = re.compile(CONFIG["REGEX_DATE"])
RE_MONTANT = re.compile(CONFIG["REGEX_MONTANT"])
RE_ANCRES_POLICE = [_compiler_ancre(a) for a in CONFIG["ANCRES_POLICE"]]
RE_ANCRES_VALIDITE = [_compiler_ancre(a) for a in CONFIG["ANCRES_VALIDITE"]]
RE_ANCRES_PRIME = [_compiler_ancre(a) for a in CONFIG["ANCRES_PRIME"]]
RE_ANCRES_ASSURE = [_compiler_ancre(a) for a in CONFIG["ANCRES_ASSURE"]]


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


def _chercher(ancre, norme, norme_inv):
    """Teste une ancre sur la ligne normalisee ET sur la ligne RETOURNEE :
    l'OCR restitue souvent l'arabe en ordre visuel inverse (RTL)."""
    return ancre.search(norme) or ancre.search(norme_inv)


def _a_ancre_police(norme, norme_inv):
    return any(_chercher(a, norme, norme_inv) for a in RE_ANCRES_POLICE)


def _candidat_chiffres(texte):
    """Concatene les groupes de chiffres d'un segment : l'OCR coupe parfois
    « 123 456 789 012 345 » en plusieurs blocs."""
    return "".join(re.findall(r"\d+", texte or ""))


def _est_bloc_numerique(texte):
    """Vrai si la ligne n'est essentiellement qu'un bloc de chiffres (separateurs
    espace/point/tiret toleres) — la forme d'un numero de police imprime seul."""
    return bool(re.fullmatch(r"[\d .\-]+", (texte or "").strip()))


# ---------------------------------------------------------------------------
# Extracteurs de champs (chacun renvoie (valeur, index_ligne) ou (None, None))
# ---------------------------------------------------------------------------

def _extraire_quittance(textes, normes, normes_inv):
    """8 chiffres apres « N° ». JAMAIS sur une ligne portant une ancre de police
    (le N° qui y figure est le numero de POLICE, pas la quittance). REPLI : l'OCR
    separe parfois le libelle du numero -> un bloc de 8 chiffres SEUL sur sa ligne,
    adjacent (FENETRE_QUITTANCE) a une ligne « N »/« N° »/« quittance », est retenu."""
    for i, brut in enumerate(textes):
        if _a_ancre_police(normes[i], normes_inv[i]):
            continue
        m = RE_QUITTANCE.search(brut)
        if m:
            return m.group(1), i

    # Repli : bloc de 8 chiffres isole, voisin d'une ligne-libelle « N »/« quittance ».
    gabarit = re.compile(r"(?<![\d\-])(\d{%d})(?![\d\-])" % CONFIG["LONGUEUR_QUITTANCE"])
    for i, brut in enumerate(textes):
        if not _est_bloc_numerique(brut):
            continue
        m = gabarit.fullmatch(brut.strip())
        if not m:
            continue
        debut = max(0, i - CONFIG["FENETRE_QUITTANCE"])
        fin = min(len(textes), i + 1 + CONFIG["FENETRE_QUITTANCE"])
        for j in range(debut, fin):
            if j == i:
                continue
            if RE_LIGNE_N.match(textes[j]) or "quittance" in normes[j]:
                return m.group(1), i
    return None, None


def _collecter_occurrences_police(textes, normes, normes_inv, quittance):
    """Toutes les lectures du numero de police. Sources :
    1) une ANCRE (fr, ou arabe eventuellement inversee) avec le numero sur la ligne,
       ou dans les FENETRE_POLICE lignes en dessous PUIS au-dessus (mise en page RTL) ;
    2) le libelle « Assuré : » de la quittance, suivi du numero (meme ligne ou ligne
       suivante) — c'est la 2e occurrence du document reel ;
    3) STRUCTUREL : une ligne qui n'est qu'un grand bloc de chiffres proche de
       LONGUEUR_POLICE (les libelles sont souvent illisibles sur une photo réelle).
    Deduplication par LIGNE SOURCE des chiffres : une meme occurrence physique captee
    deux fois ne compte qu'une (pas de fausse concordance). Le numero de quittance est
    un repere NEGATIF : jamais retenu."""

    def _rejete(c):
        return len(c) == CONFIG["LONGUEUR_QUITTANCE"] or (quittance and c == quittance)

    occurrences = {}  # {index_ligne_source: chiffres}

    def _fenetre(i, pas):
        """Premiere ligne de chiffres exploitable au-dessus (pas=-1) ou en dessous
        (pas=+1) de l'ancre i ; on s'arrete sur une autre ancre. Une ligne de DATES
        ou un petit bloc (< LONGUEUR_MIN_FENETRE) n'est pas un candidat."""
        j = i + pas
        for _ in range(CONFIG["FENETRE_POLICE"]):
            if j < 0 or j >= len(textes) or _a_ancre_police(normes[j], normes_inv[j]):
                return None, None
            if not RE_DATE.search(textes[j]):
                c = _candidat_chiffres(textes[j])
                if c and not _rejete(c) and len(c) >= CONFIG["LONGUEUR_MIN_FENETRE"]:
                    return c, j
            j += pas
        return None, None

    # 1) Ancres police (fr / arabe, y compris inversee)
    for i, ligne in enumerate(normes):
        m = None
        for ancre in RE_ANCRES_POLICE:
            m = _chercher(ancre, ligne, normes_inv[i])
            if m:
                break
        if not m:
            continue
        brut = textes[i]
        cand, source = _candidat_chiffres(brut), i  # chiffres de la ligne (avant/apres l'ancre)
        if cand and len(cand) < CONFIG["LONGUEUR_MIN_FENETRE"]:
            cand = ""  # bruit court sur la ligne du libelle : chercher le vrai numero en fenetre
        if not cand:
            cand, source = _fenetre(i, +1)
        if not cand:
            cand, source = _fenetre(i, -1)
        if not cand or _rejete(cand):
            continue
        occurrences.setdefault(source, cand)

    # 2) « Assuré : » suivi du numero (meme ligne, ou ligne suivante si l'OCR a coupe)
    for i, ligne in enumerate(normes):
        for ancre in RE_ANCRES_ASSURE:
            m = _chercher(ancre, ligne, normes_inv[i])
            if not m:
                continue
            apres = textes[i][m.end():] if m.re.search(ligne) else textes[i]
            if not re.search(r"[a-zA-Z؀-ۿ]", apres or ""):
                c = _candidat_chiffres(apres)
                if c and not _rejete(c):
                    occurrences.setdefault(i, c)
                elif not c and i + 1 < len(textes) and _est_bloc_numerique(textes[i + 1]):
                    c2 = _candidat_chiffres(textes[i + 1])
                    if c2 and not _rejete(c2) and len(c2) >= CONFIG["LONGUEUR_MIN_FENETRE"]:
                        occurrences.setdefault(i + 1, c2)
            break

    # 3) Candidats STRUCTURELS : lignes qui ne sont qu'un grand bloc de chiffres.
    tolerance = CONFIG["TOLERANCE_STRUCTURELLE"]
    borne_basse = CONFIG["LONGUEUR_POLICE"] - tolerance
    borne_haute = CONFIG["LONGUEUR_POLICE"] + 1
    for i, brut in enumerate(textes):
        if i in occurrences or not _est_bloc_numerique(brut):
            continue
        c = _candidat_chiffres(brut)
        if c and not _rejete(c) and borne_basse <= len(c) <= borne_haute:
            occurrences.setdefault(i, c)

    return sorted(occurrences.items())  # [(index_ligne_source, chiffres)] en ordre de lecture


def _arbitrer_police(candidats):
    """CONCORDANCE PAR MAJORITE. Renvoie (numero|None, ok, degre) :
    - ok=True  : >= 2 lectures IDENTIQUES de 15 chiffres (les lectures parasites d'un
      OCR bruite n'annulent pas un accord net) ;
    - degre "moyen" : une seule lecture de 15 chiffres (concordance non verifiable) ;
    - degre "bas"   : lectures divergentes, ou uniquement hors format.
    MODE ASSISTE : des qu'au moins une lecture existe, le numero est RENVOYE (l'agent
    le corrige a l'ecran) — mais ok/degre plafonnent la confiance : on n'affirme rien."""
    valides = [c for c in candidats if len(c) == CONFIG["LONGUEUR_POLICE"]]
    anomalies = [c for c in candidats if len(c) != CONFIG["LONGUEUR_POLICE"]]
    comptes = {}
    for c in valides:
        comptes[c] = comptes.get(c, 0) + 1
    if comptes:
        majoritaire = max(comptes, key=lambda c: (comptes[c], -valides.index(c)))
        if comptes[majoritaire] >= 2:
            return majoritaire, True, "ok"
        if len(comptes) > 1:
            return valides[0], False, "bas"    # divergence : renvoyee pour correction, jamais affirmee
        return valides[0], False, "moyen"      # une seule lecture au bon format
    if anomalies:
        return anomalies[0], False, "bas"      # hors format (ex. 14 chiffres) : renvoyee a corriger
    return None, False, "bas"                  # introuvable


def _extraire_immatriculation(textes):
    for i, brut in enumerate(textes):
        m = RE_IMMAT.search(brut)
        if m:
            return re.sub(r"\D", "", m.group(1)), i  # compactee : chiffres seuls
    return None, None


def _extraire_code_agence(textes):
    for i, brut in enumerate(textes):
        m = RE_AGENCE.search(brut)
        if m:
            return re.sub(r"\s", "", m.group(1)), i  # normalise : « 40. AR.0105 » -> « 40.AR.0105 »
    return None, None


def _extraire_validite(textes, normes, normes_inv):
    """« Effet du JJ/MM/AAAA Au JJ/MM/AAAA » (ou libelle arabe, eventuellement inverse).
    Les dates sont collectees AUTOUR de l'ancre (au-dessus ET en dessous : mise en page RTL,
    impression carbone decalee), puis classees CHRONOLOGIQUEMENT : du = la plus ancienne,
    au = la plus recente — l'ordre visuel n'est pas fiable (l'arabe se lit de droite a gauche)."""
    for i, ligne in enumerate(normes):
        if not any(_chercher(a, ligne, normes_inv[i]) for a in RE_ANCRES_VALIDITE):
            continue
        trouvees = []
        debut = max(0, i - CONFIG["FENETRE_DATES"])
        fin = min(len(textes), i + 1 + CONFIG["FENETRE_DATES"])
        for j in range(debut, fin):
            trouvees.extend(RE_DATE.findall(textes[j]))
        if not trouvees:
            continue
        cles = sorted({(int(annee), int(mois), int(jour)) for jour, mois, annee in trouvees})
        du = "%02d/%02d/%d" % (cles[0][2], cles[0][1], cles[0][0])
        au = "%02d/%02d/%d" % (cles[-1][2], cles[-1][1], cles[-1][0]) if len(cles) >= 2 else None
        return du, au, i
    return None, None, None


def _extraire_prime(textes, normes, normes_inv):
    """Montant decimal a virgule proche d'un libelle TTC/Prime. Normalise sans
    separateurs de milliers : « 2 400,97 » -> « 2400,97 »."""
    for ancre in RE_ANCRES_PRIME:  # ordre CONFIG = priorite (TTC avant Prime)
        for i, ligne in enumerate(normes):
            if not _chercher(ancre, ligne, normes_inv[i]):
                continue
            for j in range(i, min(i + 1 + CONFIG["FENETRE_PRIME"], len(textes))):
                montants = RE_MONTANT.findall(textes[j])
                if montants:
                    entier, centimes = montants[-1]  # le montant suit le libelle
                    return re.sub(r"[ . ]", "", entier) + "," + centimes, j
    return None, None


def _extraire_assure(textes, normes, normes_inv):
    """Nom de l'assure apres le libelle « Assuré » / « المؤمن له » (meme ligne,
    sinon ligne suivante). Une valeur SANS lettres est ignoree (sur la quittance
    reelle, « Assuré : » est suivi du NUMERO DE POLICE) ; une ligne-libelle du
    formulaire n'est jamais un nom."""
    def _est_un_nom(valeur):
        return bool(valeur) and bool(re.search(r"[a-zA-Z؀-ۿ]", valeur))

    def _est_un_libelle(norme, norme_inv):
        ancres = RE_ANCRES_POLICE + RE_ANCRES_VALIDITE + RE_ANCRES_PRIME + RE_ANCRES_ASSURE
        return any(_chercher(a, norme, norme_inv) for a in ancres)

    def _nettoyer(valeur):
        # Nettoie les restes de libelle : « (e) », « (ة) », separateurs, espaces.
        return re.sub(r"^\s*(\((e|ة)\))?\s*[:.\-–—|]*\s*", "", valeur or "", flags=re.IGNORECASE).strip()

    for i, ligne in enumerate(normes):
        for ancre in RE_ANCRES_ASSURE:
            direct = ancre.search(ligne)
            inverse = None if direct else ancre.search(normes_inv[i])
            if not direct and not inverse:
                continue
            valeur = _nettoyer(textes[i][direct.end():]) if direct else ""
            if not _est_un_nom(valeur):
                # Nom sur une ligne VOISINE : la suivante (OCR coupe libelle/valeur) ; et aussi la
                # PRECEDENTE quand le libelle est arabe inverse (RTL : le nom sort avant le libelle).
                voisins = [i + 1] + ([i - 1] if inverse else [])
                for k in voisins:
                    if 0 <= k < len(textes) and not _est_un_libelle(normes[k], normes_inv[k]):
                        candidat = _nettoyer(textes[k])
                        # Un NOM n'a jamais 4 chiffres d'affilee (ecarte les codes « pb zoui 40013- »).
                        if _est_un_nom(candidat) and not re.search(r"\d{4}", candidat):
                            valeur = candidat
                            break
            if _est_un_nom(valeur):
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
    normes_inv = [n[::-1] for n in normes]  # l'arabe ressort souvent en ordre inverse
    texte_brut = "\n".join(textes)
    utiles = set()  # index des lignes ayant fourni au moins un champ

    quittance, idx = _extraire_quittance(textes, normes, normes_inv)
    if idx is not None:
        utiles.add(idx)

    occurrences = _collecter_occurrences_police(textes, normes, normes_inv, quittance)
    utiles.update(i for i, _ in occurrences)
    police, police_ok, degre = _arbitrer_police([c for _, c in occurrences])

    immatriculation, idx = _extraire_immatriculation(textes)
    if idx is not None:
        utiles.add(idx)
    code_agence, idx = _extraire_code_agence(textes)
    if idx is not None:
        utiles.add(idx)
    valide_du, valide_au, idx = _extraire_validite(textes, normes, normes_inv)
    if idx is not None:
        utiles.add(idx)
    prime_ttc, idx = _extraire_prime(textes, normes, normes_inv)
    if idx is not None:
        utiles.add(idx)
    assure, idx = _extraire_assure(textes, normes, normes_inv)
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
