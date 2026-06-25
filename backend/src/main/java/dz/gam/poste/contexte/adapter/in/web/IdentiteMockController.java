package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
import dz.gam.poste.contexte.domain.port.out.ProfilActifStore;
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
 * API MOCK de l'identité SSO : liste les profils simulables (AGA/agents et leurs agences) et
 * permet d'en activer un POUR LA SESSION COURANTE. Simule la connexion d'utilisateurs
 * différents sans redémarrage. Demain, le profil viendra des claims du token Entra ID :
 * ce contrôleur disparaîtra, sans impact sur le domaine ni les écrans.
 */
@RestController
@RequestMapping("/api/mock/identite")
public class IdentiteMockController {

    private final ContexteProperties properties;
    private final ProfilActifStore profilActif;

    public IdentiteMockController(ContexteProperties properties, ProfilActifStore profilActif) {
        this.properties = properties;
        this.profilActif = profilActif;
    }

    @GetMapping
    public EtatIdentite etat() {
        String actif = properties.resoudre(profilActif.profilActif().orElse(null)).identifiant();
        return new EtatIdentite(actif, properties.profils().stream().map(ProfilDto::de).toList());
    }

    @PostMapping("/actif")
    public EtatIdentite activer(@Valid @RequestBody ActiverProfilRequest requete) {
        boolean existe = properties.profils().stream().anyMatch(p -> p.identifiant().equals(requete.identifiant()));
        if (!existe) {
            throw new NoSuchElementException("Profil inconnu : " + requete.identifiant());
        }
        profilActif.definir(requete.identifiant());
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
