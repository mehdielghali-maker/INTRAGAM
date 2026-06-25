package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Contexte d'agence exposé à toute l'application : l'utilisateur, l'agence active (héritée
 * par tous les écrans) et la liste des agences autorisées (limitée au périmètre SSO).
 *
 * @param agenceActive        agence active ; {@code null} en mode consolidé
 * @param consolideDisponible vue « Toutes mes agences (consolidé) » proposable
 * @param consolideActif      mode consolidé actif : vue d'ensemble en LECTURE SEULE, aucune
 *                            action possible (une action doit viser une agence précise)
 */
public record ContexteAgence(
        Utilisateur utilisateur,
        Agence agenceActive,
        List<Agence> agencesAutorisees,
        boolean consolideDisponible,
        boolean consolideActif) {
}
