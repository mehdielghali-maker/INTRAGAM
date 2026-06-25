package dz.gam.poste.contexte.domain.model;

import java.io.Serializable;

/**
 * Utilisateur authentifié pour la session courante (résultat d'une connexion réussie).
 * Pas de mot de passe ici : seulement de quoi identifier et afficher l'utilisateur.
 * {@link Serializable} car stocké en HttpSession (persistance/clustering éventuels).
 *
 * @param type       ADMIN (administration) ou AGA (espace métier)
 * @param login      identifiant de connexion saisi
 * @param nomAffiche libellé présenté dans l'interface
 */
public record Principal(TypePrincipal type, String login, String nomAffiche) implements Serializable {
}
