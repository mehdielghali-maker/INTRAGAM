"""
Tests PURS des regles d'extraction d'attestation (aucune dependance PaddleOCR).
Lancer :  pytest test_attestation_regles.py -q
Les lignes simulent la sortie OCR (texte + confiance), dans l'ordre de lecture.
"""
from attestation_regles import CONFIG, extraire_attestation


def _lignes(*textes, conf=0.95):
    return [{"texte": t, "confiance": conf} for t in textes]


# Attestation GAM nominale : le numero de police (15 chiffres) apparait DEUX fois
# (sous le libelle arabe en haut du certificat, puis sur la ligne « Police N° »).
ATTESTATION_NOMINALE = _lignes(
    "شهادة تأمين السيارات",
    "رقم عقد التأمين",
    "407020091260200",
    "N° 06681933",
    "Assuré : A. Hamdi",
    "Police N° 407020091260200",
    "Immatriculation : 502000 114 16",
    "Effet du 04/06/2026 Au 03/07/2026",
    "Prime TTC : 2 400,97",
    "Agence : 40.AR.0105",
)


def test_nominale_police_15_chiffres_concordante_statut_lu():
    r = extraire_attestation(ATTESTATION_NOMINALE)
    assert r["numeroPolice"] == "407020091260200"
    assert r["statut"] == "lu"
    assert r["confiance"] >= CONFIG["SEUIL_CONFIANCE"]


def test_nominale_tous_les_champs_extraits():
    r = extraire_attestation(ATTESTATION_NOMINALE)
    assert r["numeroQuittance"] == "06681933"
    assert r["immatriculation"] == "50200011416"  # compactee (chiffres seuls)
    assert r["assure"] == "A. Hamdi"
    assert r["valideDu"] == "04/06/2026"
    assert r["valideAu"] == "03/07/2026"
    assert r["primeTTC"] == "2400,97"             # separateur de milliers retire
    assert r["codeAgence"] == "40.AR.0105"
    assert "Police N° 407020091260200" in r["texteBrut"]


def test_quatorze_chiffres_renvoye_mais_a_verifier():
    # Les DEUX occurrences ne font que 14 chiffres : renvoye (mode assiste) mais jamais affirme.
    r = extraire_attestation(_lignes(
        "رقم عقد التأمين",
        "40702009126020",
        "Police N° 40702009126020",
    ))
    assert r["numeroPolice"] == "40702009126020"
    assert r["statut"] == "a_verifier"
    assert r["confiance"] <= CONFIG["CONFIANCE_BASSE"]


def test_occurrences_divergentes_a_verifier_confiance_basse():
    r = extraire_attestation(_lignes(
        "رقم عقد التأمين",
        "407020091260200",
        "Police N° 407020091260318",  # 15 chiffres aussi, mais DIFFERENT → divergence
    ))
    assert r["statut"] == "a_verifier"
    assert r["confiance"] <= CONFIG["CONFIANCE_BASSE"]
    assert r["numeroPolice"] in ("407020091260200", "407020091260318")


def test_lecture_unique_concordance_non_verifiable():
    # Une seule occurrence lisible : on renvoie le numero mais on ne peut pas verifier la concordance.
    r = extraire_attestation(_lignes("Police N° 407020091260200"))
    assert r["numeroPolice"] == "407020091260200"
    assert r["statut"] == "a_verifier"
    assert r["confiance"] <= CONFIG["CONFIANCE_MOYENNE"]


def test_la_quittance_nest_jamais_prise_pour_la_police():
    # Sous l'ancre arabe ne figure QUE le grand numero rouge (8 chiffres) : repere NEGATIF.
    r = extraire_attestation(_lignes(
        "رقم عقد التأمين",
        "N° 06681933",
    ))
    assert r["numeroQuittance"] == "06681933"
    assert r["numeroPolice"] is None
    assert r["statut"] == "a_verifier"


def test_confiance_ocr_basse_donne_a_verifier_malgre_concordance():
    r = extraire_attestation(_lignes(
        "رقم عقد التأمين",
        "407020091260200",
        "Police N° 407020091260200",
        conf=0.30,  # OCR douteux : concordance OK mais on invite au controle
    ))
    assert r["numeroPolice"] == "407020091260200"
    assert r["statut"] == "a_verifier"


# Mise en page du DOCUMENT RÉEL (photo GAM fournie par l'utilisateur) : le numéro de police
# est AU-DESSUS du libellé arabe (RTL) et, sur la quittance, EN FACE d'« Assuré : » ; le libellé
# « Police N° : » y est VIDE, suivi de la ligne de dates (piège de la fenêtre).
ATTESTATION_REELLE = _lignes(
    "407020091260200",
    "رقم عقد التأمين",
    "صالحة من 04/05/2026 إلى 03/07/2026",
    "N° 06681933",
    "QUITTANCE DE PRIME",
    "N° : pb zoui 40013-",
    "Assuré : 407020091260200",
    "Police N° :",
    "Effet du 04/05/2026 Au 03/07/2026",
    "Prime TTC : 2400,97DA",
    "Code Agence : 40. AR.0105",
    "502000-114-16",
)


def test_document_reel_police_concordante_malgre_la_mise_en_page():
    r = extraire_attestation(ATTESTATION_REELLE)
    # Deux occurrences RÉELLES (au-dessus du libellé arabe + en face d'« Assuré ») → concordance.
    assert r["numeroPolice"] == "407020091260200"
    assert r["statut"] == "lu"


def test_document_reel_champs_annexes():
    r = extraire_attestation(ATTESTATION_REELLE)
    assert r["numeroQuittance"] == "06681933"
    assert r["immatriculation"] == "50200011416"       # séparateurs tirets tolérés
    assert r["codeAgence"] == "40.AR.0105"             # espaces du tampon normalisés
    assert r["primeTTC"] == "2400,97"
    assert r["valideDu"] == "04/05/2026"
    assert r["valideAu"] == "03/07/2026"
    # « Assuré : » est suivi du numéro de police (pas d'un nom) et la ligne suivante est un
    # libellé : aucun nom inventé.
    assert r["assure"] is None


def test_document_reel_la_ligne_de_dates_ne_pollue_pas_la_fenetre():
    # « Police N° : » vide suivi d'« Effet du ... » : la fenêtre ne doit PAS concaténer les dates.
    r = extraire_attestation(_lignes(
        "Police N° :",
        "Effet du 04/05/2026 Au 03/07/2026",
        "Prime TTC : 2400,97DA",
    ))
    assert r["numeroPolice"] is None


# Sortie OCR RÉELLE (PaddleOCR sur la photo de l'utilisateur) : libellés COLLÉS, arabe INVERSÉ,
# « N° » séparé de son numéro, lectures multiples dont une corrompue (003 au lieu de 009).
def test_libelles_colles_et_arabe_inverse():
    r = extraire_attestation(_lignes(
        "نيمأتلا دقع مقر",              # « رقم عقد التأمين » restitué à l'envers par l'OCR
        "407020091260200",
        "PoliceN: 407020091260200",     # libellé collé (sans espace)
    ))
    assert r["numeroPolice"] == "407020091260200"
    assert r["statut"] == "lu"          # 2 lectures identiques de 15 chiffres = concordance


def test_lignes_ocr_reelles_quittance_separee_et_divergence():
    r = extraire_attestation(_lignes(
        "407020031260200",              # lecture corrompue (003) du certificat
        "نيمأتلا دع مقر",               # libellé arabe inversé ET tronqué (illisible)
        "٨٨",
        "06681933",                     # le grand numéro rouge, SÉPARÉ de son « N° »
        "NO",
        "006681933",                    # écho parasite (9 chiffres)
        "Assure:",                      # libellé coupé de sa valeur
        "407020091260200",              # la vraie police, ligne suivante
        "PoliceN:",
        "Effetdu: 2400,970AAu",         # montant collé (DA lu « 0A »)
        "19801634-80",                  # décret : ne doit devenir ni quittance ni police
        conf=0.6))
    assert r["numeroQuittance"] == "06681933"          # repli « bloc isolé voisin d'une ligne N »
    # Deux lectures 15 chiffres DIVERGENTES (003 vs 009) : renvoyée pour correction, jamais affirmée.
    assert r["numeroPolice"] in ("407020031260200", "407020091260200")
    assert r["statut"] == "a_verifier"
    assert r["confiance"] <= CONFIG["CONFIANCE_BASSE"]
    assert r["immatriculation"] is None


# 2e DOCUMENT RÉEL (vignette-certificat « وثيقة تأمين السيارة », photographiée SEULE) — lignes
# telles que RÉELLEMENT restituées par PaddleOCR : libellés arabes inversés/mangés, déchet
# « () yuatl » près du libellé, nom COLLÉ deux lignes plus loin, dates éloignées de l'ancre,
# immatriculation collée (« 066630940 » — wilaya 40), décret en ...-80 (wilaya inexistante).
ATTESTATION_VIGNETTE = _lignes(
    "ةقيثو",
    "NO06681843",
    "مقر مومسرم",
    "(08910-0861",
    "نمؤملا",                       # « المؤمن » inversé — libellé, jamais un nom
    "ديسلا(",                       # « السيد » inversé
    "() yuatl",                     # déchet OCR minuscule : jamais un nom
    "ARBAOUIRACHID",                # le vrai nom, collé, 2 lignes sous le libellé
    "pb zoui 40013",
    "0702009126016",                # lecture corrompue (13 chiffres)
    "نيمأتلا دقع مقر",              # « رقم عقد التأمين » inversé
    "407020091260196",
    "08/05/2026",
    "0805/2026",
    "07/05/2027",
    "75/2027",
    "نم ةحلاص",                     # « صالحة من » inversé
    "FOTON",
    "066630940",
    "19801634-80",
)


def test_vignette_certificat_photographiee_seule():
    r = extraire_attestation(ATTESTATION_VIGNETTE)
    assert r["numeroPolice"] == "407020091260196"
    assert r["statut"] == "a_verifier"       # UNE seule occurrence sur ce type de document (honnête)
    assert r["numeroQuittance"] == "06681843"
    assert r["assure"] == "ARBAOUIRACHID"    # ni « () yuatl » (minuscules) ni « pb zoui 40013 »
    assert r["valideDu"] == "08/05/2026"     # ordre CHRONOLOGIQUE, pas l'ordre visuel RTL
    assert r["valideAu"] == "07/05/2027"
    assert r["immatriculation"] == "066630940"  # repli « bloc collé + wilaya plausible (40) »


def test_montant_colle_reste_lisible():
    r = extraire_attestation(_lignes("PrimeTTC:", "2400,970A"))  # « 2400,97DA » mal lu
    assert r["primeTTC"] == "2400,97"


def test_un_bloc_de_chiffres_contigus_nest_pas_une_immatriculation():
    # « …16 فيفري 1980 » / numéros administratifs : 10 chiffres contigus ne font pas une plaque.
    r = extraire_attestation(_lignes("AUTORISATION N 1980163480 DU 24-1-1984"))
    assert r["immatriculation"] is None


def test_entree_vide_neutre():
    r = extraire_attestation([])
    assert r["numeroPolice"] is None
    assert r["statut"] == "a_verifier"
    assert r["confiance"] == 0.0
    assert r["texteBrut"] == ""
