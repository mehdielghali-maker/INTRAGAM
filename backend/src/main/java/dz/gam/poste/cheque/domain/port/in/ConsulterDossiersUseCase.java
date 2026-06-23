package dz.gam.poste.cheque.domain.port.in;

import dz.gam.poste.cheque.domain.model.DossierCheque;

import java.util.List;
import java.util.UUID;

/** Port d'entrée : consultation des dossiers de suivi (liste filtrable + détail). */
public interface ConsulterDossiersUseCase {

    List<DossierCheque> lister(FiltreDossier filtre);

    /** @throws dz.gam.poste.cheque.domain.model.DossierIntrouvableException si absent */
    DossierCheque obtenir(UUID idDossier);
}
