package dz.gam.poste.contexte.domain.service;

import dz.gam.poste.contexte.domain.model.ActionConsolideeInterditeException;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.model.AgenceHorsPerimetreException;
import dz.gam.poste.contexte.domain.model.ContexteAgence;
import dz.gam.poste.contexte.domain.model.GroupeAgences;
import dz.gam.poste.contexte.domain.model.Identite;
import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
import dz.gam.poste.contexte.domain.model.Utilisateur;
import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContexteAgenceServiceTest {

    // Groupe « Agence Saïd Hamdine » avec 2 sous-agences (unités d'action).
    private static final Agence HYDRA = new Agence("02.1.1", "Point Hydra");
    private static final Agence BMR = new Agence("02.1.2", "Point Bir Mourad Raïs");
    private static final GroupeAgences BENZERGA =
            new GroupeAgences("02.1.BENZERGA", "Agence Saïd Hamdine", List.of(HYDRA, BMR));
    // Agence autonome (sa propre unité d'action).
    private static final GroupeAgences DRARIA =
            new GroupeAgences("02.7.DRARIA", "Agence Draria", List.of());
    private static final Agence DRARIA_FEUILLE = new Agence("02.7.DRARIA", "Agence Draria");

    private final FauxStore store = new FauxStore();

    private ContexteAgenceService service(GroupeAgences... perimetre) {
        Utilisateur aga = new Utilisateur("m.benzerga", "M. Benzerga", ProfilUtilisateur.AGA);
        IdentitePort identite = () -> new Identite(aga, List.of(perimetre));
        return new ContexteAgenceService(identite, store);
    }

    @Test
    void atterrissage_direct_sur_la_premiere_sous_agence() {
        ContexteAgence ctx = service(BENZERGA, DRARIA).contexte();

        assertThat(ctx.agenceActive()).isEqualTo(HYDRA);          // 1re feuille du périmètre
        assertThat(ctx.selectionCode()).isEqualTo("02.1.1");
        assertThat(ctx.consolideDisponible()).isTrue();           // 3 feuilles
        assertThat(ctx.consolideActif()).isFalse();
        assertThat(ctx.perimetre()).containsExactly(BENZERGA, DRARIA);
    }

    @Test
    void selectionner_le_groupe_parent_est_une_vue_consolidee_lecture_seule() {
        ContexteAgenceService service = service(BENZERGA, DRARIA);

        ContexteAgence ctx = service.changer("02.1.BENZERGA");

        assertThat(ctx.consolideActif()).isTrue();
        assertThat(ctx.agenceActive()).isNull();
        assertThat(ctx.selectionLibelle()).isEqualTo("Agence Saïd Hamdine (consolidé)");
        // Lecture = les 2 sous-agences ; action interdite.
        assertThat(service.agencesActives()).containsExactly(HYDRA, BMR);
        assertThatThrownBy(service::agencePourAction).isInstanceOf(ActionConsolideeInterditeException.class);
    }

    @Test
    void selectionner_une_sous_agence_autorise_l_action() {
        ContexteAgenceService service = service(BENZERGA, DRARIA);

        ContexteAgence ctx = service.changer("02.1.2");

        assertThat(ctx.consolideActif()).isFalse();
        assertThat(ctx.agenceActive()).isEqualTo(BMR);
        assertThat(service.agencePourAction()).isEqualTo(BMR);
        assertThat(service.agencesActives()).containsExactly(BMR);
    }

    @Test
    void agence_autonome_est_sa_propre_unite_d_action() {
        ContexteAgenceService service = service(BENZERGA, DRARIA);

        service.changer("02.7.DRARIA");

        assertThat(service.estConsolide()).isFalse();
        assertThat(service.agencePourAction()).isEqualTo(DRARIA_FEUILLE);
    }

    @Test
    void consolide_global_couvre_toutes_les_feuilles_et_interdit_l_action() {
        ContexteAgenceService service = service(BENZERGA, DRARIA);

        ContexteAgence ctx = service.changer(ContexteAgenceService.CODE_CONSOLIDE);

        assertThat(ctx.consolideActif()).isTrue();
        assertThat(ctx.selectionLibelle()).isEqualTo("Toutes mes agences (consolidé)");
        assertThat(service.agencesActives()).containsExactly(HYDRA, BMR, DRARIA_FEUILLE);
        assertThatThrownBy(service::agencePourAction).isInstanceOf(ActionConsolideeInterditeException.class);
    }

    @Test
    void selection_hors_perimetre_est_rejetee() {
        ContexteAgenceService service = service(BENZERGA, DRARIA);

        assertThatThrownBy(() -> service.changer("99.9.INCONNUE"))
                .isInstanceOf(AgenceHorsPerimetreException.class);
        assertThat(store.codeActif()).isEmpty();
    }

    @Test
    void mono_agence_autonome_sans_consolide() {
        ContexteAgenceService service = service(DRARIA);

        ContexteAgence ctx = service.contexte();
        assertThat(ctx.consolideDisponible()).isFalse();
        assertThat(ctx.agenceActive()).isEqualTo(DRARIA_FEUILLE);
        assertThatThrownBy(() -> service.changer(ContexteAgenceService.CODE_CONSOLIDE))
                .isInstanceOf(AgenceHorsPerimetreException.class);
    }

    @Test
    void exiger_acces_porte_sur_les_feuilles() {
        ContexteAgenceService service = service(BENZERGA, DRARIA);

        service.exigerAcces("02.1.1"); // feuille du périmètre : OK
        assertThatThrownBy(() -> service.exigerAcces("02.1.BENZERGA")) // un groupe n'est pas une feuille
                .isInstanceOf(AgenceHorsPerimetreException.class);
    }

    private static final class FauxStore implements AgenceActiveStore {
        private String code;

        @Override
        public Optional<String> codeActif() {
            return Optional.ofNullable(code);
        }

        @Override
        public void definir(String codeAgence) {
            this.code = codeAgence;
        }
    }
}
