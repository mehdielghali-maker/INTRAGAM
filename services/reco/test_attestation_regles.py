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


def test_entree_vide_neutre():
    r = extraire_attestation([])
    assert r["numeroPolice"] is None
    assert r["statut"] == "a_verifier"
    assert r["confiance"] == 0.0
    assert r["texteBrut"] == ""
