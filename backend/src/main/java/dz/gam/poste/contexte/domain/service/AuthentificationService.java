package dz.gam.poste.contexte.domain.service;

import dz.gam.poste.contexte.domain.model.CompteAdmin;
import dz.gam.poste.contexte.domain.model.CompteAdminVue;
import dz.gam.poste.contexte.domain.model.IdentifiantsAga;
import dz.gam.poste.contexte.domain.model.IdentifiantsInvalidesException;
import dz.gam.poste.contexte.domain.model.MotDePasseActuelInvalideException;
import dz.gam.poste.contexte.domain.model.Principal;
import dz.gam.poste.contexte.domain.model.TypePrincipal;
import dz.gam.poste.contexte.domain.port.in.AuthentifierUseCase;
import dz.gam.poste.contexte.domain.port.in.ConsulterSessionUseCase;
import dz.gam.poste.contexte.domain.port.in.GererCompteAdminUseCase;
import dz.gam.poste.contexte.domain.port.out.CompteAdminStore;
import dz.gam.poste.contexte.domain.port.out.ComptesAgaStore;
import dz.gam.poste.contexte.domain.port.out.MotDePasseEncodeur;
import dz.gam.poste.contexte.domain.port.out.ProfilActifStore;
import dz.gam.poste.contexte.domain.port.out.SessionAuthStore;

import java.util.Optional;

/**
 * Service d'authentification (domaine pur). Connecte un ADMIN (compte unique) ou un AGA (profil
 * persisté, recherché par login), en comparant le mot de passe à son empreinte. Sur succès,
 * ouvre la session ; pour un AGA, fixe aussi le profil actif (le reste de l'app le lit déjà).
 * Gère également le compte admin (mot de passe, e-mail de récupération). Aucune dépendance
 * framework : tout passe par des ports.
 */
public class AuthentificationService
        implements AuthentifierUseCase, ConsulterSessionUseCase, GererCompteAdminUseCase {

    private static final String NOM_ADMIN = "Administrateur";

    private final CompteAdminStore compteAdmin;
    private final ComptesAgaStore comptesAga;
    private final MotDePasseEncodeur encodeur;
    private final SessionAuthStore session;
    private final ProfilActifStore profilActif;

    public AuthentificationService(CompteAdminStore compteAdmin, ComptesAgaStore comptesAga,
                                   MotDePasseEncodeur encodeur, SessionAuthStore session,
                                   ProfilActifStore profilActif) {
        this.compteAdmin = compteAdmin;
        this.comptesAga = comptesAga;
        this.encodeur = encodeur;
        this.session = session;
        this.profilActif = profilActif;
    }

    @Override
    public Principal connecter(String login, String motDePasse) {
        if (login == null || motDePasse == null) {
            throw new IdentifiantsInvalidesException();
        }
        Optional<CompteAdmin> admin = compteAdmin.charger();
        if (admin.isPresent() && admin.get().login().equalsIgnoreCase(login.trim())) {
            return connecterAdmin(admin.get(), motDePasse);
        }
        return connecterAga(login.trim(), motDePasse);
    }

    private Principal connecterAdmin(CompteAdmin admin, String motDePasse) {
        if (!encodeur.correspond(motDePasse, admin.motDePasseHash())) {
            throw new IdentifiantsInvalidesException();
        }
        Principal principal = new Principal(TypePrincipal.ADMIN, admin.login(), NOM_ADMIN);
        session.definir(principal);
        return principal;
    }

    private Principal connecterAga(String login, String motDePasse) {
        IdentifiantsAga aga = comptesAga.trouverParLogin(login)
                .filter(a -> a.motDePasseHash() != null && encodeur.correspond(motDePasse, a.motDePasseHash()))
                .orElseThrow(IdentifiantsInvalidesException::new);
        Principal principal = new Principal(TypePrincipal.AGA, aga.login(), aga.nomAffiche());
        session.definir(principal);
        profilActif.definir(aga.identifiant()); // le contexte d'agence dérive du profil actif
        return principal;
    }

    @Override
    public Optional<Principal> sessionCourante() {
        return session.principal();
    }

    @Override
    public void deconnecter() {
        session.effacer();
    }

    @Override
    public CompteAdminVue consulter() {
        CompteAdmin a = compteAdminObligatoire();
        return new CompteAdminVue(a.login(), a.emailRecuperation());
    }

    @Override
    public void changerMotDePasse(String ancien, String nouveau) {
        CompteAdmin a = compteAdminObligatoire();
        if (!encodeur.correspond(ancien == null ? "" : ancien, a.motDePasseHash())) {
            throw new MotDePasseActuelInvalideException();
        }
        if (nouveau == null || nouveau.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe est requis");
        }
        compteAdmin.enregistrer(new CompteAdmin(a.login(), encodeur.encoder(nouveau), a.emailRecuperation()));
    }

    @Override
    public void definirEmailRecuperation(String email) {
        CompteAdmin a = compteAdminObligatoire();
        String valeur = (email == null || email.isBlank()) ? null : email.trim();
        compteAdmin.enregistrer(new CompteAdmin(a.login(), a.motDePasseHash(), valeur));
    }

    @Override
    public String indiceRecuperation() {
        return masquer(compteAdmin.charger().map(CompteAdmin::emailRecuperation).orElse(null));
    }

    private CompteAdmin compteAdminObligatoire() {
        return compteAdmin.charger()
                .orElseThrow(() -> new IllegalStateException("Compte admin non initialisé"));
    }

    /** Masque un e-mail : {@code mehdi@gmail.com} → {@code m***@g***.com}. Null si absent. */
    private static String masquer(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int arobase = email.indexOf('@');
        if (arobase <= 0 || arobase == email.length() - 1) {
            return "***";
        }
        String local = email.substring(0, arobase);
        String domaine = email.substring(arobase + 1);
        int point = domaine.lastIndexOf('.');
        String ext = point >= 0 ? domaine.substring(point) : "";
        return local.charAt(0) + "***@" + domaine.charAt(0) + "***" + ext;
    }
}
