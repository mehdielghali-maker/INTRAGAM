package dz.gam.poste.versement.domain.model;

/** Versement inexistant pour l'identifiant ou la référence donnés. Traduite en HTTP 404. */
public class VersementIntrouvableException extends RuntimeException {

    public VersementIntrouvableException(String cle) {
        super("Versement introuvable : " + cle);
    }
}
