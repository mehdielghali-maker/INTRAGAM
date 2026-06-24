package dz.gam.poste.cotation.domain.port.in;

import dz.gam.poste.cotation.domain.model.DemandeCotation;

import java.util.UUID;

/** Port d'entrée : clôture « sans suite » d'une demande (devis non retenu). */
public interface ClotureDemandeUseCase {

    DemandeCotation marquerSansSuite(UUID idDemande, String motif);
}
