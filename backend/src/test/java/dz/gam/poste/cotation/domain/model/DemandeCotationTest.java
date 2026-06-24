package dz.gam.poste.cotation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemandeCotationTest {

    private static final Instant T0 = Instant.parse("2026-06-23T10:00:00Z");

    private DemandeCotation brouillon(String numeroPolice) {
        return DemandeCotation.creerBrouillon("Auto — flotte", "SARL Yassir", numeroPolice, "Devis VTC",
                "16.I.Gharbi", "DR Alger-Est", "M. Saïdi", List.of(), T0);
    }

    @Test
    void brouillon_sans_reference_ni_evenement() {
        DemandeCotation d = brouillon(null);
        assertThat(d.statut()).isEqualTo(StatutCotation.BROUILLON);
        assertThat(d.reference()).isNull();
        assertThat(d.renouvellement()).isFalse();
        assertThat(d.evenementsNonPublies()).isEmpty();
    }

    @Test
    void police_renseignee_marque_un_renouvellement() {
        assertThat(brouillon("P-10044").renouvellement()).isTrue();
    }

    @Test
    void envoyer_affecte_reference_passe_envoyee_et_emet_evenement() {
        DemandeCotation d = brouillon(null);
        d.envoyer(new ReferenceDemande("DC-2026-0413"), T0);

        assertThat(d.statut()).isEqualTo(StatutCotation.ENVOYEE);
        assertThat(d.reference().valeur()).isEqualTo("DC-2026-0413");
        assertThat(d.evenementsNonPublies()).hasSize(1);
        assertThat(d.souscripteur()).isNull(); // « Non affecté » avant prise en charge
    }

    @Test
    void cycle_complet_jusqu_a_affaire_gagnee() {
        DemandeCotation d = brouillon(null);
        d.envoyer(new ReferenceDemande("DC-2026-0413"), T0);
        d.prendreEnCharge(new Souscripteur("K. Bensalem"), T0);
        assertThat(d.statut()).isEqualTo(StatutCotation.EN_COURS);
        assertThat(d.souscripteur().initiales()).isEqualTo("KB");

        d.finaliser("PR-88231", "DV-88231", T0);
        assertThat(d.statut()).isEqualTo(StatutCotation.A_FINALISER);
        assertThat(d.numeroProposition()).isEqualTo("PR-88231");

        d.marquerAffaireGagnee("P-10044", T0);
        assertThat(d.statut()).isEqualTo(StatutCotation.AFFAIRE_GAGNEE);
        assertThat(d.numeroPolice()).isEqualTo("P-10044");
        assertThat(d.dateQuittance()).isEqualTo(T0);
    }

    @Test
    void sans_suite_depuis_a_finaliser() {
        DemandeCotation d = brouillon(null);
        d.envoyer(new ReferenceDemande("DC-2026-0413"), T0);
        d.prendreEnCharge(new Souscripteur("K. Bensalem"), T0);
        d.finaliser("PR-1", "DV-1", T0);
        d.marquerSansSuite(null, T0);

        assertThat(d.statut()).isEqualTo(StatutCotation.SANS_SUITE);
        assertThat(d.motifSansSuite()).isEqualTo("Devis non retenu par le client");
    }

    @Test
    void transition_interdite_refusee() {
        DemandeCotation d = brouillon(null);
        assertThatThrownBy(() -> d.prendreEnCharge(new Souscripteur("X"), T0))
                .isInstanceOf(TransitionCotationInvalideException.class);
    }
}
