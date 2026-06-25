package dz.gam.poste.contexte.domain.model;

/**
 * Identifiants de connexion d'un profil AGA/agent (lus pour authentifier). L'{@code identifiant}
 * est la clé technique interne (ex. {@code m.benzerga}) ; le {@code login} est saisi à la
 * connexion (ex. {@code benzerga}). Le mot de passe n'est connu que par son empreinte.
 *
 * @param identifiant    clé technique du profil
 * @param login          identifiant de connexion saisi par l'AGA
 * @param nomAffiche     libellé de l'utilisateur
 * @param motDePasseHash empreinte BCrypt, ou null si aucun mot de passe défini
 */
public record IdentifiantsAga(String identifiant, String login, String nomAffiche, String motDePasseHash) {
}
