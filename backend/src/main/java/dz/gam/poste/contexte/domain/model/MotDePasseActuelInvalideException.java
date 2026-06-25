package dz.gam.poste.contexte.domain.model;

/**
 * Changement de mot de passe refusé : le mot de passe actuel fourni ne correspond pas.
 * Traduite en HTTP 400.
 */
public class MotDePasseActuelInvalideException extends RuntimeException {

    public MotDePasseActuelInvalideException() {
        super("Mot de passe actuel incorrect");
    }
}
