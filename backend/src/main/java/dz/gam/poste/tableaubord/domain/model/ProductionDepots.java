package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;

/**
 * Bloc « Production & dépôts du mois » : du chiffre émis à l'argent en banque.
 * {@code ecart} = encaissé − versé (cohérent avec la carte KPI « Écart à régulariser »).
 */
public record ProductionDepots(
        BigDecimal production,
        BigDecimal encaisse,
        BigDecimal depose,
        BigDecimal ecart) {
}
