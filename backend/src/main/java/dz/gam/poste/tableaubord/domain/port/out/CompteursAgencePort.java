package dz.gam.poste.tableaubord.domain.port.out;

/**
 * Port de sortie fournissant les compteurs « à traiter ». L'adapter agrège la source
 * RÉELLE des chèques (workflow du poste) et des mocks pour les fonctions non encore
 * implémentées.
 */
public interface CompteursAgencePort {

    CompteursAgence compteurs();
}
