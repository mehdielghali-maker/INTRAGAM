package dz.gam.poste.cotation.domain.port.out;

import dz.gam.poste.cotation.domain.event.DemandeCotationEmiseEvent;

/** Port de sortie : publication de l'émission de la demande vers le central (bus). */
public interface PublicationCotationPort {

    void publier(DemandeCotationEmiseEvent evenement);
}
