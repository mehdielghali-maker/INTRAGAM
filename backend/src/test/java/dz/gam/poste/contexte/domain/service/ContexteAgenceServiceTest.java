package dz.gam.poste.contexte.domain.service;

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

    private static final Agence SAID_HAMDINE = new Agence("02.1.S.BENZERGA", "Agence Saïd Hamdine");
    private static final Agence AISSAT_IDIR = new Agence("02.4.Aïssat Idir", "Agence Aïssat Idir");
    private static final Agence DRARIA = new Agence("02.7.Draria", "Agence Draria");

    private final FauxStore store = new FauxStore();

    private ContexteAgenceService service(Agence... perimetre) {
        Utilisateur aga = new Utilisateur("m.benzerga", "M. Benzerga", ProfilUtilisateur.AGA);
        IdentitePort identite = () -> new Identite(aga, List.of(perimetre));
        return new ContexteAgenceService(identite, store);
    }

    @Test
    void atterrissage_direct_sans_choix_actif_la_premiere_agence_du_perimetre() {
        ContexteAgence ctx = service(SAID_HAMDINE, AISSAT_IDIR, DRARIA).contexte();

        assertThat(ctx.agenceActive()).isEqualTo(SAID_HAMDINE);
        assertThat(ctx.agencesAutorisees()).containsExactly(SAID_HAMDINE, AISSAT_IDIR, DRARIA);
        assertThat(ctx.consolideDisponible()).isFalse();
        assertThat(ctx.utilisateur().profil()).isEqualTo(ProfilUtilisateur.AGA);
    }

    @Test
    void changement_vers_une_agence_du_perimetre_devient_active_et_est_memorise() {
        ContexteAgenceService service = service(SAID_HAMDINE, AISSAT_IDIR, DRARIA);

        ContexteAgence ctx = service.changer("02.7.Draria");

        assertThat(ctx.agenceActive()).isEqualTo(DRARIA);
        assertThat(store.codeActif()).contains("02.7.Draria");
        // La lecture suivante reflète bien le choix mémorisé en session.
        assertThat(service.agenceActive()).isEqualTo(DRARIA);
    }

    @Test
    void changement_vers_une_agence_hors_perimetre_est_rejete() {
        ContexteAgenceService service = service(SAID_HAMDINE, AISSAT_IDIR);

        assertThatThrownBy(() -> service.changer("99.9.INCONNUE"))
                .isInstanceOf(AgenceHorsPerimetreException.class)
                .hasMessageContaining("99.9.INCONNUE");
        // Aucun effet de bord : l'agence active n'a pas changé.
        assertThat(store.codeActif()).isEmpty();
    }

    @Test
    void exiger_acces_rejette_hors_perimetre_et_accepte_dans_le_perimetre() {
        ContexteAgenceService service = service(SAID_HAMDINE, AISSAT_IDIR);

        assertThatThrownBy(() -> service.exigerAcces("02.7.Draria"))
                .isInstanceOf(AgenceHorsPerimetreException.class);
        // Dans le périmètre : aucune exception.
        service.exigerAcces("02.4.Aïssat Idir");
    }

    @Test
    void agent_mono_agence_sans_friction() {
        ContexteAgence ctx = service(SAID_HAMDINE).contexte();

        assertThat(ctx.agencesAutorisees()).containsExactly(SAID_HAMDINE);
        assertThat(ctx.agenceActive()).isEqualTo(SAID_HAMDINE);
    }

    @Test
    void un_code_actif_devenu_hors_perimetre_retombe_sur_la_premiere_agence() {
        store.definir("02.7.Draria"); // plus dans le périmètre ci-dessous
        ContexteAgence ctx = service(SAID_HAMDINE, AISSAT_IDIR).contexte();

        assertThat(ctx.agenceActive()).isEqualTo(SAID_HAMDINE);
    }

    /** Store en mémoire (équivalent test du bean session-scoped). */
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
