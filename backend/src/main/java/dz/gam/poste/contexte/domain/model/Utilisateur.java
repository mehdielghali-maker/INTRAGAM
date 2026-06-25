package dz.gam.poste.contexte.domain.model;

/**
 * Utilisateur connecté (identité SSO Microsoft/Entra ID). Le nom affiché et le profil
 * alimentent la barre supérieure ; le profil borne le périmètre d'agences géré.
 */
public record Utilisateur(String identifiant, String nomAffiche, ProfilUtilisateur profil) {
}
