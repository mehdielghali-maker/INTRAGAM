package dz.gam.poste.dpd.integration;

import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.StatutDpd;
import dz.gam.poste.dpd.domain.model.TypePersonne;
import dz.gam.poste.dpd.domain.port.in.ConsulterDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.CreerDpdCommand;
import dz.gam.poste.dpd.domain.port.in.EnregistrerDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.dpd.domain.port.in.MiseAJourDpdUseCase;
import dz.gam.poste.dpd.domain.port.out.AccordSuiviRepository;
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

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Boucle DPD de bout en bout (bus + base) : envoi → prise en charge validateur (mock BPM)
 * → accord validé + échéancier v1 (mock PROASSUR) ; puis resynchronisation (v2, historisée).
 * Délais du mock central forcés à 0. Nécessite Docker (sinon ignoré).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BoucleDpdIT {

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
        registry.add("poste.dpd.mock.delai-validation-ms", () -> 0);
        registry.add("poste.dpd.mock.delai-accord-ms", () -> 0);
    }

    @Autowired
    EnregistrerDpdUseCase enregistrer;
    @Autowired
    ConsulterDpdUseCase consulter;
    @Autowired
    MiseAJourDpdUseCase miseAJour;
    @Autowired
    AccordSuiviRepository accords;

    @Test
    void envoi_validation_puis_accord_avec_echeancier_versionne() {
        DemandeDpd demande = enregistrer.envoyer(new CreerDpdCommand(
                "PR-88231", false, "DPD 6 mois", "SARL Méditerranée", "SARL Méditerranée",
                "021", "RC123", TypePersonne.MORALE, false, "Alger",
                List.of("rc.pdf"), List.of()));
        String reference = demande.reference().valeur();

        DemandeDpd[] accordee = new DemandeDpd[1];
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            DemandeDpd maj = parReference(reference);
            assertThat(maj).isNotNull();
            assertThat(maj.statut()).isEqualTo(StatutDpd.ACCORDEE);
            assertThat(maj.validateur()).isNotNull();      // remonté par le BPM
            assertThat(maj.codeAccord()).isNotBlank();     // attribué par PROASSUR
            accordee[0] = maj;
        });

        // L'échéancier validé est historisé en version 1 du suivi de l'accord.
        String codeAccord = accordee[0].codeAccord();
        AccordSuivi suivi = accords.trouverParCodeAccord(codeAccord).orElseThrow();
        assertThat(suivi.versions()).hasSize(1);
        assertThat(suivi.versionCourante().total()).isEqualByComparingTo("1240000");

        // Resynchronisation (renégociation) → nouvelle version, sans écraser l'ancienne.
        miseAJour.synchroniser(codeAccord);
        AccordSuivi apres = accords.trouverParCodeAccord(codeAccord).orElseThrow();
        assertThat(apres.versions()).hasSize(2);
    }

    private DemandeDpd parReference(String reference) {
        List<DemandeDpd> demandes = consulter.lister(FiltreDpd.aucun());
        return demandes.stream()
                .filter(d -> d.reference() != null && d.reference().valeur().equals(reference))
                .findFirst().orElse(null);
    }
}
