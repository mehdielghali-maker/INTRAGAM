package dz.gam.poste.cotation.integration;

import dz.gam.poste.cotation.adapter.out.centralmock.ProassurMockQuittanceController;
import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import dz.gam.poste.cotation.domain.port.in.ConsulterDemandesUseCase;
import dz.gam.poste.cotation.domain.port.in.CreerDemandeCommand;
import dz.gam.poste.cotation.domain.port.in.EnregistrerDemandeUseCase;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
import org.junit.jupiter.api.Test;
import dz.gam.poste.integration.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Boucle aller-retour de la cotation à travers le vrai bus et la vraie base :
 * envoi (poste) → prise en charge souscripteur (mock BPM) → cotation faite (mock PROASSUR,
 * « À finaliser » + références) → impression quittance (mock PROASSUR) → « Affaire gagnée ».
 *
 * <p>Délais du mock central forcés à 0. Nécessite Docker (sinon ignoré).
 */
@SpringBootTest
class BoucleCotationIT extends IntegrationTestBase {

    @DynamicPropertySource
    static void mockProperties(DynamicPropertyRegistry registry) {
        registry.add("poste.cotation.mock.delai-prise-en-charge-ms", () -> 0);
        registry.add("poste.cotation.mock.delai-cotation-ms", () -> 0);
    }

    @Autowired
    EnregistrerDemandeUseCase enregistrer;
    @Autowired
    ConsulterDemandesUseCase consulter;
    @Autowired
    ProassurMockQuittanceController quittanceMock;

    @Test
    void envoi_puis_retours_central_jusqu_a_affaire_gagnee() {
        DemandeCotation demande = enregistrer.envoyer(new CreerDemandeCommand(
                "Auto — flotte", "SARL Yassir VTC", null, "Pack Yassir", List.of("kbis.pdf")));
        String reference = demande.reference().valeur();

        // Mock BPM puis PROASSUR : la demande progresse jusqu'à « À finaliser » avec ses références.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            DemandeCotation maj = parReference(reference);
            assertThat(maj).isNotNull();
            assertThat(maj.statut()).isEqualTo(StatutCotation.A_FINALISER);
            assertThat(maj.souscripteur()).isNotNull();              // remonté par le BPM
            assertThat(maj.numeroProposition()).isNotBlank();        // renvoyé par PROASSUR
        });

        // Impression de la quittance (mock PROASSUR) → « Affaire gagnée » + n° police.
        quittanceMock.imprimerQuittance(reference, "P-99001");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            DemandeCotation maj = parReference(reference);
            assertThat(maj.statut()).isEqualTo(StatutCotation.AFFAIRE_GAGNEE);
            assertThat(maj.numeroPolice()).isEqualTo("P-99001");
        });
    }

    private DemandeCotation parReference(String reference) {
        List<DemandeCotation> demandes = consulter.lister(FiltreDemande.aucun());
        return demandes.stream()
                .filter(d -> d.reference() != null && d.reference().valeur().equals(reference))
                .findFirst().orElse(null);
    }
}
