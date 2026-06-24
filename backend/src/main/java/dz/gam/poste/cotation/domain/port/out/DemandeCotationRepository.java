package dz.gam.poste.cotation.domain.port.out;

import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.ReferenceDemande;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de sortie : persistance de l'état des demandes (propre au poste). */
public interface DemandeCotationRepository {

    DemandeCotation enregistrer(DemandeCotation demande);

    Optional<DemandeCotation> trouverParId(UUID id);

    Optional<DemandeCotation> trouverParReference(ReferenceDemande reference);

    List<DemandeCotation> lister(FiltreDemande filtre);

    /** Nombre de demandes ayant déjà une référence (pour générer la suivante). */
    long compterReferencees();
}
