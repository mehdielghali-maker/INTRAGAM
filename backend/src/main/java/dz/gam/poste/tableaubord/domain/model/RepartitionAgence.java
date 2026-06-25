package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;

/**
 * Ligne de répartition par agence, affichée uniquement en vue consolidée (« Toutes mes
 * agences »). Donne la contribution de chaque agence aux principaux indicateurs.
 */
public record RepartitionAgence(
        String code,
        String nom,
        BigDecimal caYtd,
        BigDecimal encaisse,
        BigDecimal depose,
        BigDecimal ecart) {
}
