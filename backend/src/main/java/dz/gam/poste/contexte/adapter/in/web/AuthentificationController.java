package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.adapter.out.identite.ProfilsAdminStore;
import dz.gam.poste.contexte.domain.model.Principal;
import dz.gam.poste.contexte.domain.model.TypePrincipal;
import dz.gam.poste.contexte.domain.port.in.AuthentifierUseCase;
import dz.gam.poste.contexte.domain.port.in.ConsulterSessionUseCase;
import dz.gam.poste.contexte.domain.port.in.GererCompteAdminUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Authentification de l'utilisateur (admin ou AGA) par login + mot de passe. Endpoints NON
 * protégés (le filtre d'accès laisse passer {@code /api/auth/**}) : connexion, déconnexion,
 * état de session, configuration (SSO) et indice de récupération du mot de passe.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthentificationController {

    private final AuthentifierUseCase authentifier;
    private final ConsulterSessionUseCase session;
    private final GererCompteAdminUseCase compteAdmin;
    private final ProfilsAdminStore profils;
    private final boolean ssoMicrosoftActif;

    public AuthentificationController(AuthentifierUseCase authentifier, ConsulterSessionUseCase session,
                                      GererCompteAdminUseCase compteAdmin, ProfilsAdminStore profils,
                                      @Value("${poste.auth.sso-microsoft-actif:false}") boolean ssoMicrosoftActif) {
        this.authentifier = authentifier;
        this.session = session;
        this.compteAdmin = compteAdmin;
        this.profils = profils;
        this.ssoMicrosoftActif = ssoMicrosoftActif;
    }

    @PostMapping("/login")
    public PrincipalDto login(@Valid @RequestBody LoginRequest requete) {
        Principal p = authentifier.connecter(requete.login(), requete.motDePasse());
        return PrincipalDto.de(p);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        session.deconnecter();
    }

    @GetMapping("/etat")
    public ResponseEntity<PrincipalDto> etat() {
        return session.sessionCourante()
                .map(p -> ResponseEntity.ok(PrincipalDto.de(p)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/config")
    public ConfigAuth config() {
        return new ConfigAuth(ssoMicrosoftActif);
    }

    /**
     * Comptes sélectionnables sur l'écran de connexion (sans mot de passe) : l'administrateur en
     * tête, puis les AGA/agents disposant d'un login. Endpoint public (page de login).
     */
    @GetMapping("/comptes")
    public List<CompteConnectable> comptes() {
        List<CompteConnectable> liste = new ArrayList<>();
        liste.add(new CompteConnectable(compteAdmin.consulter().login(), "Administrateur", TypePrincipal.ADMIN));
        profils.lister().stream()
                .filter(p -> p.login() != null && !p.login().isBlank())
                .forEach(p -> liste.add(new CompteConnectable(p.login(), p.nomAffiche(), TypePrincipal.AGA)));
        return liste;
    }

    @PostMapping("/mot-de-passe-oublie")
    public RecuperationDto motDePasseOublie() {
        String indice = compteAdmin.indiceRecuperation();
        String message = indice == null
                ? "Aucune adresse de récupération n'est configurée. Contactez l'exploitant."
                : "Un lien de réinitialisation sera envoyé à l'adresse de récupération (intégration e-mail à venir).";
        return new RecuperationDto(indice, message);
    }

    public record LoginRequest(@NotBlank String login, @NotBlank String motDePasse) {
    }

    public record PrincipalDto(TypePrincipal role, String login, String nomAffiche) {
        static PrincipalDto de(Principal p) {
            return new PrincipalDto(p.type(), p.login(), p.nomAffiche());
        }
    }

    public record ConfigAuth(boolean ssoMicrosoftActif) {
    }

    public record CompteConnectable(String login, String libelle, TypePrincipal type) {
    }

    public record RecuperationDto(String indiceEmail, String message) {
    }
}
