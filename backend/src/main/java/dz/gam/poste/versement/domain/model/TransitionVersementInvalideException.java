package dz.gam.poste.versement.domain.model;

/** Transition de statut non autorisée par le cycle de vie. Traduite en HTTP 409. */
public class TransitionVersementInvalideException extends RuntimeException {

    public TransitionVersementInvalideException(StatutVersement actuel, StatutVersement cible) {
        super("Transition invalide : " + actuel + " → " + cible);
    }
}
