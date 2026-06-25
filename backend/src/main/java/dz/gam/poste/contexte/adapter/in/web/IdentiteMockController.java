package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.adapter.out.identite.ProfilsAdminStore;
import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.model.Principal;
import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
import dz.gam.poste.contexte.domain.model.TypePrincipal;
import dz.gam.poste.contexte.domain.port.out.ProfilActifStore;
import dz.gam.poste.contexte.domain.port.out.SessionAuthStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Aperçu/impersonation d'un profil AGA depuis l'administration (réservé ADMIN, cf. interceptor).
 * Liste les profils et permet d'« activer » l'un d'eux POUR LA SESSION COURANTE : l'admin voit
 * alors l'espace de cet AGA (le contexte d'agence dérive du profil actif). Pour revenir à
 * l'administration, l'admin se déconnecte et se reconnecte. Demain, le profil viendra d'Entra ID.
 */
@RestController
@RequestMapping("/api/admin/identite")
public class IdentiteMockController {

    private final ProfilsAdminStore profils;
    private final ProfilActifStore profilActif;
    private final SessionAuthStore sessionAuth;

    public IdentiteMockController(ProfilsAdminStore profils, ProfilActifStore profilActif,
                                  SessionAuthStore sessionAuth) {
        this.profils = profils;
        this.profilActif = profilActif;
        this.sessionAuth = sessionAuth;
    }

    @GetMapping
    public EtatIdentite etat() {
        return new EtatIdentite(profilActif.profilActif().orElse(null),
                profils.lister().stream().map(ProfilDto::de).toList());
    }

    @PostMapping("/actif")
    public EtatIdentite activer(@Valid @RequestBody ActiverProfilRequest requete) {
        ContexteProperties.Profil p = profils.trouver(requete.identifiant())
                .orElseThrow(() -> new NoSuchElementException("Profil inconnu : " + requete.identifiant()));
        profilActif.definir(p.identifiant());
        // Impersonation : l'admin devient cet AGA le temps de l'aperçu (login fallback = identifiant).
        String login = (p.login() == null || p.login().isBlank()) ? p.identifiant() : p.login();
        sessionAuth.definir(new Principal(TypePrincipal.AGA, login, p.nomAffiche()));
        return etat();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail introuvable(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    public record ActiverProfilRequest(@NotBlank String identifiant) {
    }

    public record EtatIdentite(String profilActif, List<ProfilDto> profils) {
    }

    public record ProfilDto(String identifiant, String nomAffiche, ProfilUtilisateur profil, List<AgenceDto> agences) {
        static ProfilDto de(ContexteProperties.Profil p) {
            return new ProfilDto(p.identifiant(), p.nomAffiche(), p.profil(),
                    p.agences().stream().map(a -> new AgenceDto(a.code(), a.nom())).toList());
        }
    }

    public record AgenceDto(String code, String nom) {
    }
}
