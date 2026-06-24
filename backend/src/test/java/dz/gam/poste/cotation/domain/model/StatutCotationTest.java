package dz.gam.poste.cotation.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatutCotationTest {

    @Test
    void chemin_nominal_autorise() {
        assertThat(StatutCotation.BROUILLON.peutTransitionnerVers(StatutCotation.ENVOYEE)).isTrue();
        assertThat(StatutCotation.ENVOYEE.peutTransitionnerVers(StatutCotation.EN_COURS)).isTrue();
        assertThat(StatutCotation.EN_COURS.peutTransitionnerVers(StatutCotation.A_FINALISER)).isTrue();
        assertThat(StatutCotation.A_FINALISER.peutTransitionnerVers(StatutCotation.AFFAIRE_GAGNEE)).isTrue();
        assertThat(StatutCotation.A_FINALISER.peutTransitionnerVers(StatutCotation.SANS_SUITE)).isTrue();
    }

    @Test
    void terminaux_et_actifs() {
        assertThat(StatutCotation.AFFAIRE_GAGNEE.estTerminal()).isTrue();
        assertThat(StatutCotation.SANS_SUITE.estTerminal()).isTrue();

        assertThat(StatutCotation.ENVOYEE.estActive()).isTrue();
        assertThat(StatutCotation.EN_COURS.estActive()).isTrue();
        assertThat(StatutCotation.A_FINALISER.estActive()).isTrue();
        assertThat(StatutCotation.BROUILLON.estActive()).isFalse();
        assertThat(StatutCotation.AFFAIRE_GAGNEE.estActive()).isFalse();
        assertThat(StatutCotation.SANS_SUITE.estActive()).isFalse();
    }

    @Test
    void sauts_interdits() {
        assertThat(StatutCotation.BROUILLON.peutTransitionnerVers(StatutCotation.EN_COURS)).isFalse();
        assertThat(StatutCotation.ENVOYEE.peutTransitionnerVers(StatutCotation.A_FINALISER)).isFalse();
        assertThat(StatutCotation.EN_COURS.peutTransitionnerVers(StatutCotation.AFFAIRE_GAGNEE)).isFalse();
    }
}
