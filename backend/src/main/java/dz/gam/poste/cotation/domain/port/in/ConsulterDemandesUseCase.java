package dz.gam.poste.cotation.domain.port.in;

import dz.gam.poste.cotation.domain.model.DemandeCotation;

import java.util.List;
import java.util.UUID;

/** Port d'entrée : consultation des demandes de cotation. */
public interface ConsulterDemandesUseCase {

    List<DemandeCotation> lister(FiltreDemande filtre);

    DemandeCotation obtenir(UUID idDemande);

    /** Nombre de demandes « en cours » (alimente le badge « Cotations en cours » de l'accueil). */
    long compterActives();
}
