package dz.gam.poste.versement.integration;

import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.in.ConsulterVersementsUseCase;
import dz.gam.poste.versement.domain.port.in.DeposerVersementCommand;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.in.SoumettreVersementUseCase;
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
 * Boucle aller-retour du versement à travers le vrai bus et la vraie base :
 * soumission (poste) → « En contrôle » (mock BPM) → « Validé » (mock BPM).
 *
 * <p>Délais du mock BPM forcés à 0, décision VALIDE. Nécessite Docker (sinon ignoré).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BoucleVersementIT {

    private static final String AGENCE = "02.1.S.BENZERGA";

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
        registry.add("poste.versement.mock.delai-controle-ms", () -> 0);
        registry.add("poste.versement.mock.delai-decision-ms", () -> 0);
        registry.add("poste.versement.mock.decision", () -> "VALIDE");
    }

    @Autowired
    SoumettreVersementUseCase soumettre;
    @Autowired
    ConsulterVersementsUseCase consulter;

    @Test
    void soumission_puis_retours_bpm_jusqu_a_valide() {
        Versement depose = soumettre.soumettre(AGENCE, "M. Benzerga", new DeposerVersementCommand(
                "2026-06", BigDecimal.valueOf(1_450_000), LocalDate.parse("2026-06-24"),
                "BRD-3391", "AGB", "Versement de juin", List.of("recu-juin.pdf")));
        String reference = depose.reference();

        assertThat(depose.statut()).isEqualTo(StatutVersement.DEPOSE);

        // Mock BPM : « En contrôle » puis « Validé » via le bus.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Versement maj = parReference(reference);
            assertThat(maj).isNotNull();
            assertThat(maj.statut()).isEqualTo(StatutVersement.VALIDE);
            assertThat(maj.referenceBpm()).isNotBlank();
        });
    }

    private Versement parReference(String reference) {
        List<Versement> versements = consulter.lister(FiltreVersement.parAgence(AGENCE));
        return versements.stream()
                .filter(v -> reference.equals(v.reference()))
                .findFirst().orElse(null);
    }
}
