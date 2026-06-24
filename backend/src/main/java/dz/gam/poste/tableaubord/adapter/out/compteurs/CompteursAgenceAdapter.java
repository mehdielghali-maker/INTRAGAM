package dz.gam.poste.tableaubord.adapter.out.compteurs;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.cotation.domain.port.in.ConsulterDemandesUseCase;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgencePort;
import org.springframework.stereotype.Component;

/**
 * Fournit les compteurs « à traiter ». Compteurs RÉELS (source unique) agrégés des
 * workflows du poste déjà livrés : chèques non terminaux et cotations en cours. Les
 * autres compteurs restent mockés en attendant l'implémentation de leurs fonctions.
 */
@Component
public class CompteursAgenceAdapter implements CompteursAgencePort {

    private final ConsulterDossiersUseCase consulterCheques;
    private final ConsulterDemandesUseCase consulterCotations;

    public CompteursAgenceAdapter(ConsulterDossiersUseCase consulterCheques,
                                  ConsulterDemandesUseCase consulterCotations) {
        this.consulterCheques = consulterCheques;
        this.consulterCotations = consulterCotations;
    }

    @Override
    public CompteursAgence compteurs() {
        int chequesEnAttente = (int) consulterCheques.lister(FiltreDossier.aucun()).stream()
                .map(DossierCheque::statut)
                .filter(statut -> !statut.estTerminal())
                .count();
        int cotationsEnCours = (int) consulterCotations.compterActives();

        // Mocks (fonctions non encore implémentées) — cohérents avec la maquette.
        return new CompteursAgence(
                chequesEnAttente,
                7,                   // attestations
                cotationsEnCours,    // cotations (réel)
                3,                   // échéanciers à risque
                9,                   // contentieux
                2,                   // envois bureau d'ordre
                5,                   // demandes d'expertise
                2);                  // accords d'échéancier
    }
}
