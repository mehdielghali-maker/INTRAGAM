package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;

/**
 * Carte KPI exprimée en montant (DA) avec une variation comparée et sa ligne de
 * référence. Utilisée pour CA YTD, CA du mois et Créances non recouvrées.
 */
public record CarteMontant(
        BigDecimal valeur,
        Variation variation,
        BigDecimal reference,
        String libelleReference) {
}
