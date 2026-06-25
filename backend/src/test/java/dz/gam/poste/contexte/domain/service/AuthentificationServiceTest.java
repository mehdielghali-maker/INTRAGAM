package dz.gam.poste.contexte.domain.service;

import dz.gam.poste.contexte.domain.model.CompteAdmin;
import dz.gam.poste.contexte.domain.model.CompteAdminVue;
import dz.gam.poste.contexte.domain.model.IdentifiantsAga;
import dz.gam.poste.contexte.domain.model.IdentifiantsInvalidesException;
import dz.gam.poste.contexte.domain.model.MotDePasseActuelInvalideException;
import dz.gam.poste.contexte.domain.model.Principal;
import dz.gam.poste.contexte.domain.model.TypePrincipal;
import dz.gam.poste.contexte.domain.port.out.CompteAdminStore;
import dz.gam.poste.contexte.domain.port.out.ComptesAgaStore;
import dz.gam.poste.contexte.domain.port.out.MotDePasseEncodeur;
import dz.gam.poste.contexte.domain.port.out.ProfilActifStore;
import dz.gam.poste.contexte.domain.port.out.SessionAuthStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthentificationServiceTest {

    private final FauxCompteAdmin compteAdmin = new FauxCompteAdmin();
    private final FauxComptesAga comptesAga = new FauxComptesAga();
    private final FauxEncodeur encodeur = new FauxEncodeur();
    private final FauxSession session = new FauxSession();
    private final FauxProfilActif profilActif = new FauxProfilActif();

    private final AuthentificationService service =
            new AuthentificationService(compteAdmin, comptesAga, encodeur, session, profilActif);

    @Test
    void connexion_admin_ouvre_une_session_admin() {
        compteAdmin.enregistrer(new CompteAdmin("admin", encodeur.encoder("admin"), null));

        Principal p = service.connecter("admin", "admin");

        assertThat(p.type()).isEqualTo(TypePrincipal.ADMIN);
        assertThat(p.login()).isEqualTo("admin");
        assertThat(session.principal()).contains(p);
        assertThat(profilActif.identifiant).isNull(); // l'admin n'active aucun profil AGA
    }

    @Test
    void connexion_admin_mot_de_passe_errone_est_rejetee() {
        compteAdmin.enregistrer(new CompteAdmin("admin", encodeur.encoder("admin"), null));

        assertThatThrownBy(() -> service.connecter("admin", "mauvais"))
                .isInstanceOf(IdentifiantsInvalidesException.class);
        assertThat(session.principal()).isEmpty();
    }

    @Test
    void connexion_aga_par_login_ouvre_la_session_et_fixe_le_profil_actif() {
        compteAdmin.enregistrer(new CompteAdmin("admin", encodeur.encoder("admin"), null));
        comptesAga.ajouter(new IdentifiantsAga("m.benzerga", "benzerga", "M. Benzerga", encodeur.encoder("gam2026")));

        Principal p = service.connecter("benzerga", "gam2026");

        assertThat(p.type()).isEqualTo(TypePrincipal.AGA);
        assertThat(p.nomAffiche()).isEqualTo("M. Benzerga");
        assertThat(session.principal()).contains(p);
        assertThat(profilActif.identifiant).isEqualTo("m.benzerga");
    }

    @Test
    void connexion_aga_mot_de_passe_errone_est_rejetee() {
        comptesAga.ajouter(new IdentifiantsAga("m.benzerga", "benzerga", "M. Benzerga", encodeur.encoder("gam2026")));

        assertThatThrownBy(() -> service.connecter("benzerga", "mauvais"))
                .isInstanceOf(IdentifiantsInvalidesException.class);
        assertThat(session.principal()).isEmpty();
        assertThat(profilActif.identifiant).isNull();
    }

    @Test
    void connexion_login_inconnu_est_rejetee() {
        assertThatThrownBy(() -> service.connecter("inconnu", "x"))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }

    @Test
    void aga_sans_mot_de_passe_ne_peut_pas_se_connecter() {
        comptesAga.ajouter(new IdentifiantsAga("m.saidi", "saidi", "M. Saïdi", null));

        assertThatThrownBy(() -> service.connecter("saidi", "x"))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }

    @Test
    void changement_de_mot_de_passe_admin_verifie_l_ancien() {
        compteAdmin.enregistrer(new CompteAdmin("admin", encodeur.encoder("admin"), null));

        assertThatThrownBy(() -> service.changerMotDePasse("mauvais", "nouveau"))
                .isInstanceOf(MotDePasseActuelInvalideException.class);

        service.changerMotDePasse("admin", "nouveau");
        // L'ancien ne fonctionne plus, le nouveau oui.
        assertThatThrownBy(() -> service.connecter("admin", "admin"))
                .isInstanceOf(IdentifiantsInvalidesException.class);
        assertThat(service.connecter("admin", "nouveau").type()).isEqualTo(TypePrincipal.ADMIN);
    }

    @Test
    void email_de_recuperation_est_enregistre_et_renvoye_masque() {
        compteAdmin.enregistrer(new CompteAdmin("admin", encodeur.encoder("admin"), null));

        service.definirEmailRecuperation("mehdi@gmail.com");

        CompteAdminVue vue = service.consulter();
        assertThat(vue.login()).isEqualTo("admin");
        assertThat(vue.emailRecuperation()).isEqualTo("mehdi@gmail.com");
        assertThat(service.indiceRecuperation()).isEqualTo("m***@g***.com");
    }

    @Test
    void deconnexion_efface_la_session() {
        compteAdmin.enregistrer(new CompteAdmin("admin", encodeur.encoder("admin"), null));
        service.connecter("admin", "admin");

        service.deconnecter();

        assertThat(service.sessionCourante()).isEmpty();
    }

    // --- Fakes ---

    /** Encodeur déterministe sans crypto : empreinte = "h:" + clair. */
    private static final class FauxEncodeur implements MotDePasseEncodeur {
        @Override
        public String encoder(String clair) {
            return "h:" + clair;
        }

        @Override
        public boolean correspond(String clair, String empreinte) {
            return empreinte != null && empreinte.equals("h:" + clair);
        }
    }

    private static final class FauxCompteAdmin implements CompteAdminStore {
        private CompteAdmin compte;

        @Override
        public Optional<CompteAdmin> charger() {
            return Optional.ofNullable(compte);
        }

        @Override
        public void enregistrer(CompteAdmin compte) {
            this.compte = compte;
        }
    }

    private static final class FauxComptesAga implements ComptesAgaStore {
        private final java.util.Map<String, IdentifiantsAga> parLogin = new java.util.HashMap<>();

        void ajouter(IdentifiantsAga aga) {
            parLogin.put(aga.login(), aga);
        }

        @Override
        public Optional<IdentifiantsAga> trouverParLogin(String login) {
            return Optional.ofNullable(parLogin.get(login));
        }
    }

    private static final class FauxSession implements SessionAuthStore {
        private Principal principal;

        @Override
        public Optional<Principal> principal() {
            return Optional.ofNullable(principal);
        }

        @Override
        public void definir(Principal principal) {
            this.principal = principal;
        }

        @Override
        public void effacer() {
            this.principal = null;
        }
    }

    private static final class FauxProfilActif implements ProfilActifStore {
        private String identifiant;

        @Override
        public Optional<String> profilActif() {
            return Optional.ofNullable(identifiant);
        }

        @Override
        public void definir(String identifiantProfil) {
            this.identifiant = identifiantProfil;
        }
    }
}
