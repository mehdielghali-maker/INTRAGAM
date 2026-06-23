package dz.gam.poste.cheque.integration;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.StatutCheque;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.FaireAvancerStatutUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.proassurmock.JournalWriteBackProassur;
import dz.gam.poste.proassurmock.ProassurMockPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test de bout en bout de la BOUCLE FERMÉE, à travers le vrai bus et la vraie base :
 *
 * <ol>
 *   <li>l'adapter PROASSUR mock émet {@code ChequeEmis} sur RabbitMQ ;</li>
 *   <li>le poste consomme l'événement et ouvre un dossier de suivi (PostgreSQL) ;</li>
 *   <li>on fait avancer le statut jusqu'à ENCAISSE ;</li>
 *   <li>le poste publie {@code ChequeStatutFinalise} ;</li>
 *   <li>l'adapter PROASSUR mock journalise le write-back.</li>
 * </ol>
 *
 * <p>Nécessite Docker (Testcontainers). Automatiquement ignoré sinon.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BoucleFermeeIT {

    @Container
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("poste").withUsername("poste").withPassword("poste");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired
    ProassurMockPublisher proassurMock;
    @Autowired
    ConsulterDossiersUseCase consulter;
    @Autowired
    FaireAvancerStatutUseCase faireAvancer;
    @Autowired
    JournalWriteBackProassur journalWriteBack;

    @Test
    void evenement_entrant_puis_changement_de_statut_referme_la_boucle_vers_proassur() {
        String reference = "CHQ-2026-9001";

        // (1) PROASSUR (mock) émet le chèque sur le bus.
        proassurMock.emettre(new ProassurMockPublisher.EmettreChequeRequest(
                reference, new BigDecimal("150000.00"), "DZD",
                "SARL BATICONSTRUCT", "Alger-Centre", LocalDate.of(2026, 6, 23)));

        // (2) Le poste consomme l'événement et ouvre un dossier (asynchrone).
        DossierCheque dossier = await().atMost(Duration.ofSeconds(10))
                .until(() -> trouverParReference(reference), d -> d != null);
        assertThat(dossier.statut()).isEqualTo(StatutCheque.EMIS);

        // (3) On fait avancer le statut jusqu'au terminal ENCAISSE.
        faireAvancer.faireAvancer(dossier.id(), StatutCheque.IMPRIME);
        faireAvancer.faireAvancer(dossier.id(), StatutCheque.REMIS_AGENCE);
        faireAvancer.faireAvancer(dossier.id(), StatutCheque.REMIS_BENEFICIAIRE);
        faireAvancer.faireAvancer(dossier.id(), StatutCheque.ENCAISSE);

        // (4 + 5) Le write-back arrive chez PROASSUR (mock) via le bus.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(journalWriteBack.writeBacks())
                        .anySatisfy(wb -> {
                            assertThat(wb.reference()).isEqualTo(reference);
                            assertThat(wb.statutFinal()).isEqualTo(StatutCheque.ENCAISSE.name());
                        }));
    }

    private DossierCheque trouverParReference(String reference) {
        List<DossierCheque> dossiers = consulter.lister(FiltreDossier.aucun());
        return dossiers.stream()
                .filter(d -> d.reference().valeur().equals(reference))
                .findFirst()
                .orElse(null);
    }
}
