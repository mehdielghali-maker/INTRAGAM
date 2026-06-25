package dz.gam.poste.contexte.domain.model;

/**
 * Compte d'administration du poste (unique). Login fixe (« admin »), mot de passe HASHÉ
 * modifiable, et adresse e-mail de récupération facultative (mot de passe oublié).
 *
 * @param login            identifiant de connexion (fixe : « admin »)
 * @param motDePasseHash   empreinte BCrypt du mot de passe (jamais le clair)
 * @param emailRecuperation adresse de récupération, ou null si non renseignée
 */
public record CompteAdmin(String login, String motDePasseHash, String emailRecuperation) {
}
