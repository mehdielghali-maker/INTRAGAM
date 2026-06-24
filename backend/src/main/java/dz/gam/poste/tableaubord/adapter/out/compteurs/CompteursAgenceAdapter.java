package dz.gam.poste.tableaubord.adapter.out.compteurs;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.cotation.domain.port.in.ConsulterDemandesUseCase;
import dz.gam.poste.dpd.domain.port.in.ConsulterDpdUseCase;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgencePort;
import org.springframework.stereotype.Component;

/**
 * Fournit les compteurs « à traiter ». Compteurs RÉELS (source unique) agrégés des
 * workflows du poste déjà livrés : chèques non terminaux, cotations en cours et accords
 * d'échéancier en cours. Les autres compteurs restent mockés en attendant leurs fonctions.
 */
@Component
public class CompteursAgenceAdapter implements CompteursAgencePort {

    private final ConsulterDossiersUseCase consulterCheques;
    private final ConsulterDemandesUseCase consulterCotations;
    private final ConsulterDpdUseCase consulterDpd;

    public CompteursAgenceAdapter(ConsulterDossiersUseCase consulterCheques,
                                  ConsulterDemandesUseCase consulterCotations,
                                  ConsulterDpdUseCase consulterDpd) {
        this.consulterCheques = consulterCheques;
        this.consulterCotations = consulterCotations;
        this.consulterDpd = consulterDpd;
    }

    @Override
    public CompteursAgence compteurs() {
        int chequesEnAttente = (int) consulterCheques.lister(FiltreDossier.aucun()).stream()
                .map(DossierCheque::statut)
                .filter(statut -> !statut.estTerminal())
                .count();
        int cotationsEnCours = (int) consulterCotations.compterActives();
        int accordsEnCours = (int) consulterDpd.compterActives();

        // Mocks (fonctions non encore implémentées) — cohérents avec la maquette.
        return new CompteursAgence(
                chequesEnAttente,
                7,                   // attestations
                cotationsEnCours,    // cotations (réel)
                3,                   // échéanciers à risque
                9,                   // contentieux
                2,                   // envois bureau d'ordre
                5,                   // demandes d'expertise
                accordsEnCours);     // accords d'échéancier (réel)
    }
}
