package dz.gam.poste.cheque.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatutChequeTest {

    @Test
    void le_chemin_nominal_est_autorise_de_bout_en_bout() {
        assertThat(StatutCheque.EMIS.peutTransitionnerVers(StatutCheque.IMPRIME)).isTrue();
        assertThat(StatutCheque.IMPRIME.peutTransitionnerVers(StatutCheque.REMIS_AGENCE)).isTrue();
        assertThat(StatutCheque.REMIS_AGENCE.peutTransitionnerVers(StatutCheque.REMIS_BENEFICIAIRE)).isTrue();
        assertThat(StatutCheque.REMIS_BENEFICIAIRE.peutTransitionnerVers(StatutCheque.ENCAISSE)).isTrue();
        assertThat(StatutCheque.REMIS_BENEFICIAIRE.peutTransitionnerVers(StatutCheque.RETOURNE)).isTrue();
    }

    @Test
    void seuls_encaisse_et_retourne_sont_terminaux() {
        assertThat(StatutCheque.ENCAISSE.estTerminal()).isTrue();
        assertThat(StatutCheque.RETOURNE.estTerminal()).isTrue();

        assertThat(StatutCheque.EMIS.estTerminal()).isFalse();
        assertThat(StatutCheque.IMPRIME.estTerminal()).isFalse();
        assertThat(StatutCheque.REMIS_AGENCE.estTerminal()).isFalse();
        assertThat(StatutCheque.REMIS_BENEFICIAIRE.estTerminal()).isFalse();
    }

    @Test
    void on_ne_peut_pas_sauter_une_etape() {
        assertThat(StatutCheque.EMIS.peutTransitionnerVers(StatutCheque.ENCAISSE)).isFalse();
        assertThat(StatutCheque.EMIS.peutTransitionnerVers(StatutCheque.REMIS_AGENCE)).isFalse();
    }

    @Test
    void on_ne_peut_pas_revenir_en_arriere() {
        assertThat(StatutCheque.IMPRIME.peutTransitionnerVers(StatutCheque.EMIS)).isFalse();
        assertThat(StatutCheque.REMIS_BENEFICIAIRE.peutTransitionnerVers(StatutCheque.IMPRIME)).isFalse();
    }

    @Test
    void un_statut_terminal_n_a_aucune_suite() {
        assertThat(StatutCheque.ENCAISSE.prochainsStatuts()).isEmpty();
        assertThat(StatutCheque.RETOURNE.prochainsStatuts()).isEmpty();
    }
}
