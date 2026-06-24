package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.ReferenceDpd;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de sortie : persistance des demandes DPD (état propre au poste). */
public interface DemandeDpdRepository {

    DemandeDpd enregistrer(DemandeDpd demande);

    Optional<DemandeDpd> trouverParId(UUID id);

    Optional<DemandeDpd> trouverParReference(ReferenceDpd reference);

    List<DemandeDpd> lister(FiltreDpd filtre);

    long compterReferencees();
}
