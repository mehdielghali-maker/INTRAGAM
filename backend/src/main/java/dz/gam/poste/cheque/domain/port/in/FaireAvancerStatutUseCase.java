package dz.gam.poste.cheque.domain.port.in;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.StatutCheque;

import java.util.UUID;

/**
 * Port d'entrée : faire avancer le statut d'un dossier. Si le statut cible est
 * terminal, le service publie {@code ChequeStatutFinalise} (boucle fermée).
 */
public interface FaireAvancerStatutUseCase {

    DossierCheque faireAvancer(UUID idDossier, StatutCheque statutCible);
}
