package dz.gam.poste.cheque.domain.model;

/**
 * Levée lorsqu'on tente une transition de statut non autorisée par le cycle de vie
 * (voir {@link StatutCheque}). Exception métier : traduite en HTTP 409 par l'adapter web.
 */
public class TransitionStatutInvalideException extends RuntimeException {

    public TransitionStatutInvalideException(StatutCheque actuel, StatutCheque cible) {
        super("Transition de statut invalide : " + actuel + " → " + cible
                + ". Transitions autorisées depuis " + actuel + " : " + actuel.prochainsStatuts());
    }
}
