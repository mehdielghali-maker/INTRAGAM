package dz.gam.poste.cheque.domain.model;

import java.util.UUID;

/** Levée quand aucun dossier de suivi ne correspond à l'identifiant demandé. HTTP 404. */
public class DossierIntrouvableException extends RuntimeException {

    public DossierIntrouvableException(UUID id) {
        super("Aucun dossier de chèque pour l'identifiant " + id);
    }
}
