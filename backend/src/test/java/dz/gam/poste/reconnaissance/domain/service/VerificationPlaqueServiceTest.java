package dz.gam.poste.reconnaissance.domain.service;

import dz.gam.poste.reconnaissance.domain.model.StatutVerification;
import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort.ResultatReco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logique pure de comparaison plaque ↔ contrat : aucun Spring, aucun Docker.
 */
class VerificationPlaqueServiceTest {

    private VerificationPlaqueService service;

    @BeforeEach
    void init() {
        service = new VerificationPlaqueService();
    }

    private static ResultatReco vehiculeAvecPlaque(String plaque) {
        return new ResultatReco(true, "voiture", plaque, 0.92, 0.96);
    }

    @Test
    void plaque_lue_egale_immatriculation_du_contrat_est_conforme() {
        StatutVerification statut = service.evaluer(vehiculeAvecPlaque("0987611616"), "0987611616", "avant");
        assertThat(statut).isEqualTo(StatutVerification.CONFORME);
    }

    @Test
    void plaque_normalisee_avant_comparaison_espaces_ignores() {
        // Plaque algérienne « 09876 116 16 » vs immatriculation contrat sans séparateurs.
        StatutVerification statut = service.evaluer(vehiculeAvecPlaque("09876 116 16"), "0987611616", "arriere");
        assertThat(statut).isEqualTo(StatutVerification.CONFORME);
    }

    @Test
    void plaque_differente_du_contrat_est_non_conforme() {
        StatutVerification statut = service.evaluer(vehiculeAvecPlaque("0987611616"), "1234567890", "avant");
        assertThat(statut).isEqualTo(StatutVerification.NON_CONFORME);
    }

    @Test
    void vue_avant_sans_plaque_lisible_est_non_lue() {
        StatutVerification statut = service.evaluer(vehiculeAvecPlaque(null), "0987611616", "avant");
        assertThat(statut).isEqualTo(StatutVerification.NON_LUE);
    }

    @Test
    void resultat_absent_est_non_lue() {
        StatutVerification statut = service.evaluer(null, "0987611616", "avant");
        assertThat(statut).isEqualTo(StatutVerification.NON_LUE);
    }

    @Test
    void photo_sans_vehicule_est_pas_un_vehicule() {
        ResultatReco sansVehicule = new ResultatReco(false, null, null, 0.0, 0.10);
        StatutVerification statut = service.evaluer(sansVehicule, "0987611616", "avant");
        assertThat(statut).isEqualTo(StatutVerification.PAS_UN_VEHICULE);
    }

    @Test
    void vue_laterale_n_attend_pas_de_plaque() {
        StatutVerification statut = service.evaluer(vehiculeAvecPlaque(null), "0987611616", "gauche");
        assertThat(statut).isEqualTo(StatutVerification.VUE_SANS_PLAQUE);
    }

    @Test
    void blocage_uniquement_si_aga_et_option_active_et_non_conforme() {
        assertThat(service.bloqueValidation(StatutVerification.NON_CONFORME, true, true)).isTrue();
        assertThat(service.bloqueValidation(StatutVerification.NON_CONFORME, true, false)).isFalse();
        assertThat(service.bloqueValidation(StatutVerification.NON_CONFORME, false, true)).isFalse();
        assertThat(service.bloqueValidation(StatutVerification.CONFORME, true, true)).isFalse();
    }
}
