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

    public record Profil(String identifiant, String nomAffiche, ProfilUtilisateur profil, List<Agence> agences) {
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
