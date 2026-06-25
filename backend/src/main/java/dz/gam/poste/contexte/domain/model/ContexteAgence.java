package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Contexte d'agence exposé à toute l'application : l'utilisateur, l'agence active (héritée
 * par tous les écrans) et la liste des agences autorisées (limitée au périmètre SSO).
 *
 * @param consolideDisponible vue « Toutes mes agences (consolidé) » — prévue mais pas
 *                            encore implémentée (toujours {@code false} pour l'instant).
 */
public record ContexteAgence(
        Utilisateur utilisateur,
        Agence agenceActive,
        List<Agence> agencesAutorisees,
        boolean consolideDisponible) {
}
