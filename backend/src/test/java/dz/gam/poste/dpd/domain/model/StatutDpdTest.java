package dz.gam.poste.dpd.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatutDpdTest {

    @Test
    void chemin_nominal_et_refus() {
        assertThat(StatutDpd.BROUILLON.peutTransitionnerVers(StatutDpd.ENVOYEE)).isTrue();
        assertThat(StatutDpd.ENVOYEE.peutTransitionnerVers(StatutDpd.EN_VALIDATION)).isTrue();
        assertThat(StatutDpd.EN_VALIDATION.peutTransitionnerVers(StatutDpd.ACCORDEE)).isTrue();
        assertThat(StatutDpd.EN_VALIDATION.peutTransitionnerVers(StatutDpd.REFUSEE)).isTrue();
    }

    @Test
    void terminaux_et_actifs() {
        assertThat(StatutDpd.ACCORDEE.estTerminal()).isTrue();
        assertThat(StatutDpd.REFUSEE.estTerminal()).isTrue();
        assertThat(StatutDpd.ENVOYEE.estActive()).isTrue();
        assertThat(StatutDpd.EN_VALIDATION.estActive()).isTrue();
        assertThat(StatutDpd.BROUILLON.estActive()).isFalse();
        assertThat(StatutDpd.ACCORDEE.estActive()).isFalse();
    }

    @Test
    void sauts_interdits() {
        assertThat(StatutDpd.BROUILLON.peutTransitionnerVers(StatutDpd.EN_VALIDATION)).isFalse();
        assertThat(StatutDpd.ENVOYEE.peutTransitionnerVers(StatutDpd.ACCORDEE)).isFalse();
    }
}
