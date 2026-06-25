package dz.gam.poste.tableaubord.domain.port.out;

import java.math.BigDecimal;

/**
 * Mesures BRUTES lues dans Sage (système de référence : comptabilité). Montants en DA.
 * Servent les parts « cash » des KPI (dépôts en banque, encaissements lettrés).
 */
public record MesuresSage(
        // KPI 3 + bloc Production & dépôts — déposé en banque sur le mois
        BigDecimal deposeMois,
        // Déposé CUMULÉ (YTD) — pour l'écart « à régulariser » cumulé de la carte KPI
        BigDecimal deposeCumul,
        // KPI 4 — encaissements lettrés (viennent en diminution de l'échu PROASSUR)
        BigDecimal encaissementsLettres,
        BigDecimal encaissementsLettresM1) {
}
