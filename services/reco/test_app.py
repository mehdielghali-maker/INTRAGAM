"""
Tests unitaires du service RECO — logique PURE (sans charger les modeles ML).
L'import de `app` ne declenche PAS le chargement YOLO/ALPR (fait dans l'evenement startup),
donc ces tests tournent sans modele. Lancer dans le conteneur :  pytest services/reco
(ou : docker exec poste-reco python -m pytest /app).
"""
from app import _coerce_conf, _normaliser_plaque


def test_coerce_conf_liste_moyenne():
    # Regression : selon la version de fast-alpr, `confidence` est une LISTE par caractere.
    # Avant le fix, float(liste) levait TypeError -> 500 des qu'une plaque etait detectee.
    assert _coerce_conf([0.8, 0.6]) == 0.7
    assert _coerce_conf((1.0, 0.5)) == 0.75


def test_coerce_conf_float():
    assert _coerce_conf(0.9) == 0.9
    assert _coerce_conf(0) == 0.0


def test_coerce_conf_inexploitable():
    assert _coerce_conf(None) is None
    assert _coerce_conf([]) is None
    assert _coerce_conf("x") is None


def test_normaliser_plaque():
    assert _normaliser_plaque("09876-116-16") == "0987611616"
    assert _normaliser_plaque(" ab 12 cd ") == "AB12CD"
    assert _normaliser_plaque(None) == ""
