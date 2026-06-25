package dz.gam.poste.contexte.domain.model;

/**
 * Connexion refusée : login inconnu ou mot de passe incorrect. Volontairement indifférenciée
 * (on ne révèle pas lequel des deux est faux) → traduite en HTTP 401.
 */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Identifiant ou mot de passe incorrect");
    }
}
