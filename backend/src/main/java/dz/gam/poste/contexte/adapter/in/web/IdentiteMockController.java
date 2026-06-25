package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.adapter.out.identite.ProfilsAdminStore;
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
 * API MOCK de l'identité SSO : liste les profils simulables (AGA/agents et leurs agences,
 * lus en base) et permet d'en activer un POUR LA SESSION COURANTE. Simule la connexion
 * d'utilisateurs différents sans redémarrage. Demain, le profil viendra des claims Entra ID.
 */
@RestController
@RequestMapping("/api/mock/identite")
public class IdentiteMockController {

    private final ProfilsAdminStore profils;
    private final ContexteProperties properties;
    private final ProfilActifStore profilActif;

    public IdentiteMockController(ProfilsAdminStore profils, ContexteProperties properties,
                                  ProfilActifStore profilActif) {
        this.profils = profils;
        this.properties = properties;
        this.profilActif = profilActif;
    }

    @GetMapping
    public EtatIdentite etat() {
        return new EtatIdentite(identifiantActif(), profils.lister().stream().map(ProfilDto::de).toList());
    }

    @PostMapping("/actif")
    public EtatIdentite activer(@Valid @RequestBody ActiverProfilRequest requete) {
        if (profils.trouver(requete.identifiant()).isEmpty()) {
            throw new NoSuchElementException("Profil inconnu : " + requete.identifiant());
        }
        profilActif.definir(requete.identifiant());
        return etat();
    }

    /** Identifiant du profil actif : sélection de session, ou défaut, ou 1er disponible. */
    private String identifiantActif() {
        return profilActif.profilActif().filter(id -> profils.trouver(id).isPresent())
                .or(() -> profils.trouver(properties.profilDefaut()).map(ContexteProperties.Profil::identifiant))
                .or(() -> profils.lister().stream().findFirst().map(ContexteProperties.Profil::identifiant))
                .orElse(null);
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
