package dz.gam.poste.tableaubord.domain.port.out;

/**
 * Compteurs d'éléments « à traiter » de l'agence, pour le bloc « Coup d'œil » et les
 * badges de navigation.
 *
 * <p>{@code chequesEnAttente} est un compteur RÉEL, agrégé des workflows du poste
 * (dossiers de chèques non terminaux). Les autres sont fournis par des mocks en
 * attendant l'implémentation de leurs fonctions.
 */
public record CompteursAgence(
        int chequesEnAttente,
        int attestations,
        int cotations,
        int echeanciersRisque,
        int contentieux,
        int bureauOrdre,
        int expertises,
        int accordsEcheancier,
        int versementsEnCours) {
}
