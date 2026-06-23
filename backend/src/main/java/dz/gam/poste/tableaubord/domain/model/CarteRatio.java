package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;

/**
 * Carte KPI exprimée en ratio (%), avec variation en points. Utilisée pour le S/P
 * 12 mois glissants.
 *
 * @param valeur    ratio en % (ex. 68,4)
 * @param variation écart en points vs N-1
 * @param reference ratio de référence (N-1)
 */
public record CarteRatio(BigDecimal valeur, Variation variation, BigDecimal reference) {
}
