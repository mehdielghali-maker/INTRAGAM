package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.event.DemandePaiementDiffereEmiseEvent;
import dz.gam.poste.dpd.domain.event.EcheancierDpdMisAJourEvent;

/** Port de sortie : publication des événements DPD vers le bus. */
public interface PublicationDpdPort {

    void publier(DemandePaiementDiffereEmiseEvent evenement);

    void publier(EcheancierDpdMisAJourEvent evenement);
}
