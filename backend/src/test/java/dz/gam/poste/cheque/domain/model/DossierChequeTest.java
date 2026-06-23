package dz.gam.poste.cheque.domain.model;

import dz.gam.poste.cheque.domain.event.ChequeStatutFinaliseEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DossierChequeTest {

    private static final Instant T0 = Instant.parse("2026-06-23T10:00:00Z");

    private DossierCheque dossierEmis() {
        return DossierCheque.creerDepuisEmission(
                new ReferenceCheque("CHQ-2026-0001"),
                Montant.dinars(new BigDecimal("150000.00")),
                "SARL BATICONSTRUCT", "Alger-Centre",
                LocalDate.of(2026, 6, 23), T0);
    }

    @Test
    void un_dossier_cree_part_du_statut_emis_sans_evenement() {
        DossierCheque dossier = dossierEmis();

        assertThat(dossier.statut()).isEqualTo(StatutCheque.EMIS);
        assertThat(dossier.id()).isNotNull();
        assertThat(dossier.evenementsNonPublies()).isEmpty();
    }

    @Test
    void faire_avancer_met_a_jour_le_statut_et_l_horodatage() {
        DossierCheque dossier = dossierEmis();
        Instant t1 = T0.plusSeconds(60);

        dossier.faireAvancerVers(StatutCheque.IMPRIME, t1);

        assertThat(dossier.statut()).isEqualTo(StatutCheque.IMPRIME);
        assertThat(dossier.dateDerniereMaj()).isEqualTo(t1);
        assertThat(dossier.evenementsNonPublies()).isEmpty();
    }

    @Test
    void atteindre_un_statut_terminal_produit_l_evenement_de_boucle_fermee() {
        DossierCheque dossier = dossierEmis();
        dossier.faireAvancerVers(StatutCheque.IMPRIME, T0);
        dossier.faireAvancerVers(StatutCheque.REMIS_AGENCE, T0);
        dossier.faireAvancerVers(StatutCheque.REMIS_BENEFICIAIRE, T0);

        Instant tFinal = T0.plusSeconds(3600);
        dossier.faireAvancerVers(StatutCheque.ENCAISSE, tFinal);

        assertThat(dossier.statut()).isEqualTo(StatutCheque.ENCAISSE);
        assertThat(dossier.evenementsNonPublies()).hasSize(1);
        ChequeStatutFinaliseEvent evenement = dossier.evenementsNonPublies().get(0);
        assertThat(evenement.reference()).isEqualTo(new ReferenceCheque("CHQ-2026-0001"));
        assertThat(evenement.statutFinal()).isEqualTo(StatutCheque.ENCAISSE);
        assertThat(evenement.dateFinalisation()).isEqualTo(tFinal);
    }

    @Test
    void une_transition_interdite_est_refusee() {
        DossierCheque dossier = dossierEmis();

        assertThatThrownBy(() -> dossier.faireAvancerVers(StatutCheque.ENCAISSE, T0))
                .isInstanceOf(TransitionStatutInvalideException.class);

        assertThat(dossier.statut()).isEqualTo(StatutCheque.EMIS);
        assertThat(dossier.evenementsNonPublies()).isEmpty();
    }

    @Test
    void vider_les_evenements_les_retire() {
        DossierCheque dossier = dossierEmis();
        dossier.faireAvancerVers(StatutCheque.IMPRIME, T0);
        dossier.faireAvancerVers(StatutCheque.REMIS_AGENCE, T0);
        dossier.faireAvancerVers(StatutCheque.REMIS_BENEFICIAIRE, T0);
        dossier.faireAvancerVers(StatutCheque.RETOURNE, T0);
        assertThat(dossier.evenementsNonPublies()).hasSize(1);

        dossier.viderEvenements();

        assertThat(dossier.evenementsNonPublies()).isEmpty();
    }
}
