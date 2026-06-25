package dz.gam.poste.contexte.domain.model;

/**
 * Vue exposable du compte admin : tout sauf l'empreinte du mot de passe. Sert à présenter
 * le login et l'e-mail de récupération sans jamais laisser fuiter le hash.
 */
public record CompteAdminVue(String login, String emailRecuperation) {
}
