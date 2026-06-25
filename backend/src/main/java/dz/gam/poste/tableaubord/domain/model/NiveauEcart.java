package dz.gam.poste.tableaubord.domain.model;

/**
 * Code couleur de l'écart « à régulariser », selon le ratio écart / CA annuel extrapolé (YTD
 * annualisé). Du moins grave au plus grave : CORRECT (vert), MODERE (orange), CRITIQUE (rouge),
 * DANGER (noir). Un écart nul ou négatif (versé ≥ encaissé) est toujours CORRECT.
 */
public enum NiveauEcart {
    CORRECT,
    MODERE,
    CRITIQUE,
    DANGER
}
