package dz.gam.poste.cheque.domain.service;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.DossierIntrouvableException;
import dz.gam.poste.cheque.domain.model.StatutCheque;
import dz.gam.poste.cheque.domain.port.in.ChequeEmisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChequeServiceTest {

    private FauxDossierChequeRepository repository;
    private FauxPublicationEvenementPort publication;
    private ChequeService service;

    @BeforeEach
    void init() {
        repository = new FauxDossierChequeRepository();
        publication = new FauxPublicationEvenementPort();
        Clock horloge = Clock.fixed(Instant.parse("2026-06-23T10:00:00Z"), ZoneOffset.UTC);
        service = new ChequeService(repository, publication, horloge);
    }

    private ChequeEmisCommand commande(String reference) {
        return new ChequeEmisCommand(reference, new BigDecimal("150000.00"), "DZD",
                "SARL BATICONSTRUCT", "Alger-Centre", LocalDate.of(2026, 6, 23));
    }

    @Test
    void enregistrer_ouvre_un_dossier_au_statut_emis() {
        DossierCheque dossier = service.enregistrer(commande("CHQ-2026-0001"));

        assertThat(dossier.statut()).isEqualTo(StatutCheque.EMIS);
        assertThat(repository.trouverParId(dossier.id())).isPresent();
    }

    @Test
    void enregistrer_est_idempotent_sur_la_reference() {
        DossierCheque premier = service.enregistrer(commande("CHQ-2026-0001"));
        DossierCheque second = service.enregistrer(commande("CHQ-2026-0001"));

        assertThat(second.id()).isEqualTo(premier.id());
        assertThat(repository.lister(dz.gam.poste.cheque.domain.port.in.FiltreDossier.aucun())).hasSize(1);
    }

    @Test
    void faire_avancer_jusqu_au_terminal_publie_l_evenement() {
        UUID id = service.enregistrer(commande("CHQ-2026-0001")).id();

        service.faireAvancer(id, StatutCheque.IMPRIME);
        service.faireAvancer(id, StatutCheque.REMIS_AGENCE);
        service.faireAvancer(id, StatutCheque.REMIS_BENEFICIAIRE);
        assertThat(publication.publies).isEmpty(); // aucun statut terminal encore atteint

        DossierCheque encaisse = service.faireAvancer(id, StatutCheque.ENCAISSE);

        assertThat(encaisse.statut()).isEqualTo(StatutCheque.ENCAISSE);
        assertThat(publication.publies).hasSize(1);
        assertThat(publication.publies.get(0).statutFinal()).isEqualTo(StatutCheque.ENCAISSE);
        assertThat(publication.publies.get(0).reference().valeur()).isEqualTo("CHQ-2026-0001");
    }

    @Test
    void obtenir_un_dossier_inexistant_leve_une_exception() {
        assertThatThrownBy(() -> service.obtenir(UUID.randomUUID()))
                .isInstanceOf(DossierIntrouvableException.class);
    }
}
