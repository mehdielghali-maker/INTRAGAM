package dz.gam.poste.versement.integration;

import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.in.ConsulterVersementsUseCase;
import dz.gam.poste.versement.domain.port.in.DeposerVersementCommand;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.in.SoumettreVersementUseCase;
import dz.gam.poste.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
class BoucleVersementIT extends IntegrationTestBase {

    private static final String AGENCE = "02.1.S.BENZERGA";

    @DynamicPropertySource
    static void mockProperties(DynamicPropertyRegistry registry) {
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
