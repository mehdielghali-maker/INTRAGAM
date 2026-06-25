package dz.gam.poste.contexte.domain.port.in;

import dz.gam.poste.contexte.domain.model.CompteAdminVue;

/**
 * Cas d'usage : gérer le compte d'administration depuis la session admin — consulter (login +
 * e-mail de récupération), changer le mot de passe, définir l'e-mail de récupération. Fournit
 * aussi l'indice de récupération (e-mail masqué) pour l'écran « mot de passe oublié ».
 */
public interface GererCompteAdminUseCase {

    CompteAdminVue consulter();

    void changerMotDePasse(String ancien, String nouveau);

    void definirEmailRecuperation(String email);

    /** E-mail de récupération masqué (ex. {@code m***@g***.com}), ou null si non renseigné. */
    String indiceRecuperation();
}
