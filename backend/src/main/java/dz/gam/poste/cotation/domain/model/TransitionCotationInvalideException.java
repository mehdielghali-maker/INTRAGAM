package dz.gam.poste.cotation.domain.model;

/** Transition de statut non autorisée par la machine à états. Traduite en HTTP 409. */
public class TransitionCotationInvalideException extends RuntimeException {

    public TransitionCotationInvalideException(StatutCotation actuel, StatutCotation cible) {
        super("Transition de cotation invalide : " + actuel + " → " + cible
                + ". Transitions autorisées : " + actuel.prochainsStatuts());
    }
}
