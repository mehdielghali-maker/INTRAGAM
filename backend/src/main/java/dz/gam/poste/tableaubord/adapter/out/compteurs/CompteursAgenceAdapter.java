package dz.gam.poste.tableaubord.adapter.out.compteurs;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.cotation.domain.port.in.ConsulterDemandesUseCase;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
import dz.gam.poste.dpd.domain.port.in.ConsulterDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgencePort;
import dz.gam.poste.versement.domain.port.in.ConsulterVersementsUseCase;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fournit les compteurs « à traiter ». Compteurs RÉELS (source unique) agrégés des workflows
 * du poste, BORNÉS À L'AGENCE ACTIVE (ou à tout le périmètre en mode consolidé) — cohérent
 * avec les listes par agence. Les autres compteurs restent mockés en attendant leurs fonctions.
 */
@Component
public class CompteursAgenceAdapter implements CompteursAgencePort {

    private final ConsulterDossiersUseCase consulterCheques;
    private final ConsulterDemandesUseCase consulterCotations;
    private final ConsulterDpdUseCase consulterDpd;
    private final ConsulterVersementsUseCase consulterVersements;
    private final AgenceCouranteQuery agenceCourante;

    public CompteursAgenceAdapter(ConsulterDossiersUseCase consulterCheques,
                                  ConsulterDemandesUseCase consulterCotations,
                                  ConsulterDpdUseCase consulterDpd,
                                  ConsulterVersementsUseCase consulterVersements,
                                  AgenceCouranteQuery agenceCourante) {
        this.consulterCheques = consulterCheques;
        this.consulterCotations = consulterCotations;
        this.consulterDpd = consulterDpd;
        this.consulterVersements = consulterVersements;
        this.agenceCourante = agenceCourante;
    }

    @Override
    public CompteursAgence compteurs() {
        // Agences concernées : l'agence active, ou toutes en mode consolidé.
        Set<String> agences = agenceCourante.agencesActives().stream()
                .map(Agence::code).collect(Collectors.toSet());

        int chequesEnAttente = (int) consulterCheques.lister(FiltreDossier.aucun()).stream()
                .filter(d -> agences.contains(d.agence()) && !d.statut().estTerminal())
                .count();
        int cotationsEnCours = (int) consulterCotations.lister(FiltreDemande.aucun()).stream()
                .filter(d -> agences.contains(d.codeAgence()) && d.statut().estActive())
                .count();
        int accordsEnCours = (int) consulterDpd.lister(FiltreDpd.aucun()).stream()
                .filter(d -> agences.contains(d.codeAgence()) && d.statut().estActive())
                .count();
        int versementsEnCours = (int) agences.stream()
                .flatMap(code -> consulterVersements.lister(FiltreVersement.parAgence(code)).stream())
                .filter(v -> v.statut().estEnCours())
                .count();

        // Mocks (fonctions non encore implémentées) — placeholders, indépendants de l'agence.
        return new CompteursAgence(
                chequesEnAttente,
                7,                   // attestations
                cotationsEnCours,    // cotations (réel)
                3,                   // échéanciers à risque
                9,                   // contentieux
                2,                   // envois bureau d'ordre
                5,                   // demandes d'expertise
                accordsEnCours,      // accords d'échéancier (réel)
                versementsEnCours);  // versements bancaires en cours (réel)
    }
}
