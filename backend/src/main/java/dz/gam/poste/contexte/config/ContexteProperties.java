package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Identité SSO mockée en dev (préfixe {@code poste.contexte}) : l'utilisateur connecté et son
 * périmètre d'agences. Le périmètre est une LISTE PLATE d'agences (une agence = un point de
 * vente) ; un AGA en gère plusieurs. Jamais figé en dur — caler avec l'annuaire/Entra ID réel.
 *
 * @param utilisateur identité de l'utilisateur connecté
 * @param agences     périmètre d'agences géré (AGA → plusieurs ; agent → une seule)
 */
@ConfigurationProperties(prefix = "poste.contexte")
public record ContexteProperties(Utilisateur utilisateur, List<Agence> agences) {

    public record Utilisateur(String identifiant, String nomAffiche, ProfilUtilisateur profil) {
    }

    public record Agence(String code, String nom) {
    }
}
