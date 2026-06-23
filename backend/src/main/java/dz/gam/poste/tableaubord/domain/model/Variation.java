package dz.gam.poste.tableaubord.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Variation comparée d'un indicateur. {@code favorable} porte la sémantique métier
 * (vert = favorable, terra = défavorable) — pas la direction du chiffre : une baisse
 * du S/P ou des créances est favorable, une baisse du CA ne l'est pas.
 */
public record Variation(BigDecimal valeur, UniteVariation unite, boolean favorable) {

    /**
     * Variation en pourcentage entre une valeur et sa référence.
     *
     * @param hausseFavorable vrai si une hausse est favorable (CA), faux si une baisse
     *                        est favorable (créances)
     */
    public static Variation pourcentage(BigDecimal valeur, BigDecimal reference, boolean hausseFavorable) {
        if (reference.signum() == 0) {
            // Référence nulle : variation % non définie. On l'affiche à 0 plutôt que de
            // lever une division par zéro (cas possible avec des données réelles).
            boolean fav = hausseFavorable ? valeur.signum() >= 0 : valeur.signum() <= 0;
            return new Variation(BigDecimal.ZERO.setScale(1), UniteVariation.POURCENT, fav);
        }
        BigDecimal pct = valeur.subtract(reference)
                .divide(reference, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        boolean favorable = hausseFavorable ? valeur.compareTo(reference) >= 0
                                            : valeur.compareTo(reference) <= 0;
        return new Variation(pct, UniteVariation.POURCENT, favorable);
    }

    /**
     * Variation en points (différence simple), utilisée pour le S/P.
     *
     * @param baisseFavorable vrai si une baisse est favorable (S/P)
     */
    public static Variation points(BigDecimal valeur, BigDecimal reference, boolean baisseFavorable) {
        BigDecimal ecart = valeur.subtract(reference).setScale(1, RoundingMode.HALF_UP);
        boolean favorable = baisseFavorable ? valeur.compareTo(reference) <= 0
                                            : valeur.compareTo(reference) >= 0;
        return new Variation(ecart, UniteVariation.POINTS, favorable);
    }
}
