package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;

/**
 * Carte « Écart à régulariser » = Encaissé (PROASSUR) − Versé en banque (Sage), en CUMULÉ.
 * Le code couleur dépend du ratio écart / CA annuel extrapolé (cf. {@link NiveauEcart}).
 *
 * @param valeur      écart en DA (Encaissé − Versé), cumulé
 * @param pourcentage écart / CA annuel extrapolé, en %
 * @param niveau      code couleur (CORRECT / MODERE / CRITIQUE / DANGER)
 */
public record CarteEcart(BigDecimal valeur, BigDecimal pourcentage, NiveauEcart niveau) {
}
