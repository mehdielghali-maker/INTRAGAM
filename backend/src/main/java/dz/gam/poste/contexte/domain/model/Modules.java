package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Identifiants des modules dont l'accès est contrôlable par profil (mêmes ids que la
 * navigation front). « Accueil » n'est pas listé : il est toujours accessible. Un profil
 * dont la liste de modules est vide ne voit donc que l'accueil.
 */
public final class Modules {

    private Modules() {
    }

    /** Tous les modules métier (accès complet par défaut). */
    public static final List<String> TOUS = List.of(
            "depot", "versement", "attestations", "cheques", "bureau",
            "cotation", "expertise", "accords", "echeanciers", "contentieux");
}
