package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;

/**
 * Carte d'alerte « Écart à régulariser » = Production − Déposé.
 *
 * @param valeur       écart en DA (Production − Déposé)
 * @param pourcentage  écart / production, en %
 * @param aRegulariser vrai si l'écart dépasse le seuil de config (montant OU %) → terra
 */
public record CarteEcart(BigDecimal valeur, BigDecimal pourcentage, boolean aRegulariser) {
}
