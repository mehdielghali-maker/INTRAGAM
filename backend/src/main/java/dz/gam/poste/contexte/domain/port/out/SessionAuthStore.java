package dz.gam.poste.contexte.domain.port.out;

import dz.gam.poste.contexte.domain.model.Principal;

import java.util.Optional;

/**
 * Port de sortie : utilisateur authentifié de la session courante (HttpSession serveur).
 * Vide tant qu'aucune connexion n'a eu lieu. Aucun stockage navigateur.
 */
public interface SessionAuthStore {

    Optional<Principal> principal();

    void definir(Principal principal);

    /** Termine la session (déconnexion). */
    void effacer();
}
