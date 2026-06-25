package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Identités SSO mockées en dev (préfixe {@code poste.contexte}). Plusieurs PROFILS sont
 * définis (différents AGA / agents, chacun avec SON périmètre d'agences à plat — une agence
 * = un point de vente). Le profil actif est choisi par session (mock de la connexion SSO) ;
 * à défaut, {@code profilDefaut}. Jamais figé en dur — caler avec Entra ID réel.
 *
 * @param profilDefaut identifiant du profil utilisé tant qu'aucun n'est sélectionné
 * @param profils      profils simulables
 */
@ConfigurationProperties(prefix = "poste.contexte")
public record ContexteProperties(String profilDefaut, List<Profil> profils) {

    /**
     * @param login     login de connexion (saisi par l'AGA, distinct de l'identifiant technique)
     * @param modules   ids des modules autorisés (cf. {@code Modules.TOUS}). Null/vide en
     *                  config = accès complet (résolu au seeding).
     * @param motDePasse mot de passe EN CLAIR — uniquement en entrée (config de démo, création
     *                  / modification admin) ; hashé puis oublié par le store. Jamais retourné
     *                  en lecture (toujours null à la sortie).
     */
    public record Profil(String identifiant, String login, String nomAffiche, ProfilUtilisateur profil,
                         List<Agence> agences, List<String> modules, String motDePasse) {
    }

    public record Agence(String code, String nom) {
    }

    /** Profil actif demandé, ou le profil par défaut, ou le premier déclaré. */
    public Profil resoudre(String identifiantDemande) {
        return profils.stream().filter(p -> p.identifiant().equals(identifiantDemande)).findFirst()
                .or(() -> profils.stream().filter(p -> p.identifiant().equals(profilDefaut)).findFirst())
                .orElse(profils.get(0));
    }
}
