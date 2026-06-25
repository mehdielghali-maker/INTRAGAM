package dz.gam.poste.tableaubord.domain.port.out;

import java.math.BigDecimal;

/**
 * Mesures BRUTES lues dans PROASSUR (système de référence : core insurance).
 * Le poste les affiche / combine pour la présentation, il ne les recalcule jamais
 * comme vérité. Montants en DA.
 */
public record MesuresProassur(
        // KPI 1 — CA Year-to-Date vs N-1
        BigDecimal caYtdN,
        BigDecimal caYtdN1,
        // KPI 2 — CA du mois à date vs M-1 (même quantième)
        BigDecimal caMoisN,
        BigDecimal caMoisM1MemeQuantieme,
        // KPI 3 + bloc Production & dépôts — production émise du mois
        BigDecimal productionMois,
        BigDecimal encaisseMois,
        // Encaissé CUMULÉ (YTD) — pour l'écart « à régulariser » cumulé de la carte KPI
        BigDecimal encaisseCumul,
        // KPI 4 — créances échues non encaissées (part PROASSUR : l'échu)
        BigDecimal echuNonEncaisse,
        BigDecimal echuNonEncaisseM1,
        // KPI 5 — S/P 12 mois glissants (numérateur selon le périmètre de config)
        BigDecimal sinistres12m,
        BigDecimal primes12m,
        BigDecimal sinistres12mN1,
        BigDecimal primes12mN1,
        // Stat secondaire — contrats actifs
        int contratsActifs,
        int contratsActifsVariation) {
}
