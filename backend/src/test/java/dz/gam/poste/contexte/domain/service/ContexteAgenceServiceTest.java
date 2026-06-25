package dz.gam.poste.contexte.domain.service;

import dz.gam.poste.contexte.domain.model.ActionConsolideeInterditeException;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.model.AgenceHorsPerimetreException;
import dz.gam.poste.contexte.domain.model.ContexteAgence;
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

    // Périmètre = liste plate d'agences (une agence = un point de vente).
    private static final Agence SAID = new Agence("02.1.SAID", "Agence Saïd Hamdine");
    private static final Agence HYDRA = new Agence("02.2.HYDRA", "Agence Hydra");
    private static final Agence DRARIA = new Agence("02.7.DRARIA", "Agence Draria");

    private final FauxStore store = new FauxStore();

    private ContexteAgenceService service(Agence... perimetre) {
        Utilisateur aga = new Utilisateur("m.benzerga", "M. Benzerga", ProfilUtilisateur.AGA);
        IdentitePort identite = () -> new Identite(aga, List.of(perimetre), List.of());
        return new ContexteAgenceService(identite, store);
    }

    @Test
    void atterrissage_direct_sur_la_premiere_agence_du_perimetre() {
        ContexteAgence ctx = service(SAID, HYDRA, DRARIA).contexte();

        assertThat(ctx.agenceActive()).isEqualTo(SAID);
        assertThat(ctx.agencesAutorisees()).containsExactly(SAID, HYDRA, DRARIA);
        assertThat(ctx.consolideDisponible()).isTrue();   // multi-agence
        assertThat(ctx.consolideActif()).isFalse();
    }

    @Test
    void changement_vers_une_agence_du_perimetre_devient_active() {
        ContexteAgenceService service = service(SAID, HYDRA, DRARIA);

        ContexteAgence ctx = service.changer("02.7.DRARIA");

        assertThat(ctx.agenceActive()).isEqualTo(DRARIA);
        assertThat(service.agencePourAction()).isEqualTo(DRARIA);
        assertThat(service.agencesActives()).containsExactly(DRARIA);
    }

    @Test
    void consolide_couvre_toutes_les_agences_et_interdit_l_action() {
        ContexteAgenceService service = service(SAID, HYDRA, DRARIA);

        ContexteAgence ctx = service.changer(ContexteAgenceService.CODE_CONSOLIDE);

        assertThat(ctx.consolideActif()).isTrue();
        assertThat(ctx.agenceActive()).isNull();
        assertThat(service.agencesActives()).containsExactly(SAID, HYDRA, DRARIA);
        assertThatThrownBy(service::agencePourAction).isInstanceOf(ActionConsolideeInterditeException.class);
    }

    @Test
    void changement_hors_perimetre_est_rejete() {
        ContexteAgenceService service = service(SAID, HYDRA);

        assertThatThrownBy(() -> service.changer("99.9.INCONNUE"))
                .isInstanceOf(AgenceHorsPerimetreException.class);
        assertThat(store.codeActif()).isEmpty();
    }

    @Test
    void agent_mono_agence_sans_consolide() {
        ContexteAgence ctx = service(SAID).contexte();

        assertThat(ctx.agencesAutorisees()).containsExactly(SAID);
        assertThat(ctx.agenceActive()).isEqualTo(SAID);
        assertThat(ctx.consolideDisponible()).isFalse();
        assertThatThrownBy(() -> service(SAID).changer(ContexteAgenceService.CODE_CONSOLIDE))
                .isInstanceOf(AgenceHorsPerimetreException.class);
    }

    @Test
    void exiger_acces_rejette_hors_perimetre_et_accepte_dedans() {
        ContexteAgenceService service = service(SAID, HYDRA);

        assertThatThrownBy(() -> service.exigerAcces("02.7.DRARIA"))
                .isInstanceOf(AgenceHorsPerimetreException.class);
        service.exigerAcces("02.2.HYDRA");
    }

    @Test
    void un_code_actif_devenu_hors_perimetre_retombe_sur_la_premiere_agence() {
        store.definir("02.7.DRARIA"); // plus dans le périmètre ci-dessous
        ContexteAgence ctx = service(SAID, HYDRA).contexte();

        assertThat(ctx.agenceActive()).isEqualTo(SAID);
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
