package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Contexte d'agence exposé à toute l'application : l'utilisateur, la sélection courante et le
 * périmètre hiérarchique (agences + sous-agences) pour le commutateur.
 *
 * @param agenceActive        unité d'action sélectionnée ; {@code null} si la sélection couvre
 *                            plusieurs agences (groupe parent ou consolidé global → lecture seule)
 * @param selectionCode       code sélectionné (sous-agence, agence parente, ou « CONSOLIDE »)
 * @param selectionLibelle    libellé d'affichage de la sélection
 * @param perimetre           périmètre hiérarchique pour l'affichage du commutateur
 * @param consolideDisponible vue « Toutes mes agences (consolidé) » proposable (≥ 2 feuilles)
 * @param consolideActif      sélection en lecture seule (groupe parent ou consolidé global) :
 *                            vue d'ensemble, aucune action possible
 */
public record ContexteAgence(
        Utilisateur utilisateur,
        Agence agenceActive,
        String selectionCode,
        String selectionLibelle,
        List<GroupeAgences> perimetre,
        boolean consolideDisponible,
        boolean consolideActif) {
}
