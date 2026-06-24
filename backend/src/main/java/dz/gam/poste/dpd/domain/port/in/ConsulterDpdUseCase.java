package dz.gam.poste.dpd.domain.port.in;

import dz.gam.poste.dpd.domain.model.ContexteDpd;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.port.out.PrefillProposition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port d'entrée : consultation des demandes DPD + contexte + pré-remplissage proposition. */
public interface ConsulterDpdUseCase {

    List<DemandeDpd> lister(FiltreDpd filtre);

    DemandeDpd obtenir(UUID idDemande);

    /** Alimente le badge nav « Accords d'échéancier » (source unique). */
    long compterActives();

    ContexteDpd contexte();

    /** « Charger depuis PROASSUR » de l'onglet Nouvelle demande. */
    Optional<PrefillProposition> prefill(String noProposition);
}
