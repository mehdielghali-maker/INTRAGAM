package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Identité SSO mockée en dev (préfixe {@code poste.contexte}) : l'utilisateur connecté et
 * son périmètre d'agences. Le périmètre est une hiérarchie à 2 niveaux : des agences, dont
 * certaines ont des SOUS-AGENCES. Jamais figé en dur — caler avec l'annuaire/Entra ID réel.
 *
 * @param utilisateur identité de l'utilisateur connecté
 * @param agences     périmètre d'agences géré ; une agence peut porter des sous-agences
 */
@ConfigurationProperties(prefix = "poste.contexte")
public record ContexteProperties(Utilisateur utilisateur, List<Agence> agences) {

    public record Utilisateur(String identifiant, String nomAffiche, ProfilUtilisateur profil) {
    }

    /**
     * Agence du périmètre. Si {@code sousAgences} est vide, l'agence est autonome (elle est
     * sa propre unité d'action) ; sinon c'est un groupe et les unités d'action sont ses
     * sous-agences (l'agence parente ne sert qu'à la vue consolidée du groupe).
     */
    public record Agence(String code, String nom, List<SousAgence> sousAgences) {
    }

    public record SousAgence(String code, String nom) {
    }
}
