package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;

/**
 * Carte d'alerte « Écart à régulariser » = Encaissé (PROASSUR) − Versé en banque (Sage).
 * Même définition et même source que le « Reste à régulariser » de la fonction Versement.
 *
 * @param valeur       écart en DA (Encaissé − Versé)
 * @param pourcentage  écart / encaissé, en %
 * @param aRegulariser vrai si l'écart dépasse le seuil de config (montant OU %) → terra
 */
public record CarteEcart(BigDecimal valeur, BigDecimal pourcentage, boolean aRegulariser) {
}
