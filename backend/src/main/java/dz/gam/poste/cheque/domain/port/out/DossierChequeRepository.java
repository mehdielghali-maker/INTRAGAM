package dz.gam.poste.cheque.domain.port.out;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.ReferenceCheque;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de sortie : persistance de l'état du poste (uniquement le suivi, ADR 0004).
 * Implémenté par un adapter JPA/PostgreSQL ; le domaine ignore cette technique.
 */
public interface DossierChequeRepository {

    DossierCheque enregistrer(DossierCheque dossier);

    Optional<DossierCheque> trouverParId(UUID id);

    Optional<DossierCheque> trouverParReference(ReferenceCheque reference);

    List<DossierCheque> lister(FiltreDossier filtre);
}
