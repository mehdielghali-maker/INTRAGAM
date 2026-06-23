package dz.gam.poste.tableaubord.adapter.out.compteurs;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgencePort;
import org.springframework.stereotype.Component;

/**
 * Fournit les compteurs « à traiter ». Le compteur des chèques est RÉEL : il agrège les
 * dossiers de suivi non terminaux de la fonction « Suivi des chèques » (déjà livrée),
 * via son use case public. Les autres compteurs sont mockés en attendant
 * l'implémentation de leurs fonctions respectives.
 */
@Component
public class CompteursAgenceAdapter implements CompteursAgencePort {

    private final ConsulterDossiersUseCase consulterCheques;

    public CompteursAgenceAdapter(ConsulterDossiersUseCase consulterCheques) {
        this.consulterCheques = consulterCheques;
    }

    @Override
    public CompteursAgence compteurs() {
        int chequesEnAttente = (int) consulterCheques.lister(FiltreDossier.aucun()).stream()
                .map(DossierCheque::statut)
                .filter(statut -> !statut.estTerminal())
                .count();

        // Mocks (fonctions non encore implémentées) — cohérents avec la maquette.
        return new CompteursAgence(
                chequesEnAttente,
                7,   // attestations
                4,   // cotations
                3,   // échéanciers à risque
                9,   // contentieux
                2,   // envois bureau d'ordre
                5,   // demandes d'expertise
                2);  // accords d'échéancier
    }
}
