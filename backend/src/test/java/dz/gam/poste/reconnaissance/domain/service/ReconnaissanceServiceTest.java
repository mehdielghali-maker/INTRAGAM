package dz.gam.poste.reconnaissance.domain.service;

import dz.gam.poste.reconnaissance.domain.model.ResultatVerificationVehicule;
import dz.gam.poste.reconnaissance.domain.model.StatutVerification;
import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort;
import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort.ResultatReco;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Orchestration du service (port ANPR + comparaison + décision de blocage). Port
 * factice via lambda — domaine pur, sans Spring ni Docker.
 */
class ReconnaissanceServiceTest {

    private final VerificationPlaqueService verification = new VerificationPlaqueService();

    /** Port factice : renvoie une lecture figée, quelle que soit la photo. */
    private static ReconnaissancePort portQuiLit(String plaque) {
        return (photo, vue) -> new ResultatReco(true, "voiture", plaque, 0.92, 0.96);
    }

    @Test
    void plaque_lue_conforme_au_contrat_n_est_pas_bloquante() {
        ReconnaissanceService service = new ReconnaissanceService(portQuiLit("0987611616"), verification, true);
        ResultatVerificationVehicule r = service.verifier(new byte[]{1, 2, 3}, "avant", "0987611616");
        assertThat(r.statut()).isEqualTo(StatutVerification.CONFORME);
        assertThat(r.plaqueLue()).isEqualTo("0987611616");
        assertThat(r.estVehicule()).isTrue();
        assertThat(r.bloquant()).isFalse();
    }

    @Test
    void non_concordance_bloque_si_option_anti_fraude_active() {
        ReconnaissanceService service = new ReconnaissanceService(portQuiLit("0987611616"), verification, true);
        ResultatVerificationVehicule r = service.verifier(new byte[]{1}, "avant", "0000000000");
        assertThat(r.statut()).isEqualTo(StatutVerification.NON_CONFORME);
        assertThat(r.bloquant()).isTrue();
    }

    @Test
    void non_concordance_n_est_pas_bloquante_par_defaut() {
        ReconnaissanceService service = new ReconnaissanceService(portQuiLit("0987611616"), verification, false);
        ResultatVerificationVehicule r = service.verifier(new byte[]{1}, "avant", "0000000000");
        assertThat(r.statut()).isEqualTo(StatutVerification.NON_CONFORME);
        assertThat(r.bloquant()).isFalse();
    }

    @Test
    void service_reco_en_panne_renvoie_resultat_neutre_non_bloquant() {
        // L'adapter http renvoie ResultatReco(true, null, null, 0, 0) en cas de panne :
        // estVehicule=true mais plaque=null => NON_LUE (jamais bloquant).
        ReconnaissancePort enPanne = (photo, vue) -> new ResultatReco(true, null, null, 0.0, 0.0);
        ReconnaissanceService service = new ReconnaissanceService(enPanne, verification, true);
        ResultatVerificationVehicule r = service.verifier(new byte[]{1}, "avant", "0987611616");
        assertThat(r.statut()).isEqualTo(StatutVerification.NON_LUE);
        assertThat(r.bloquant()).isFalse();
    }
}
