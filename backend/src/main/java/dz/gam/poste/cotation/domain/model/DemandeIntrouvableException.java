package dz.gam.poste.cotation.domain.model;

/** Aucune demande de cotation ne correspond à l'identifiant/référence demandé. HTTP 404. */
public class DemandeIntrouvableException extends RuntimeException {

    public DemandeIntrouvableException(String identifiant) {
        super("Aucune demande de cotation pour " + identifiant);
    }
}
