package dz.gam.poste.versement.adapter.out.proassurmock;

import dz.gam.poste.versement.domain.model.MoisSituation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Barème fictif de la situation du mois (MOCK), ancré sur la maquette validée pour
 * l'agence de référence en Juin 2026 : production 9 200 000, encaissé 7 850 000,
 * versé 6 400 000 (DA). Pondéré par l'agence et le mois pour que les sélecteurs soient
 * réellement fonctionnels. Disparaît avec le branchement réel PROASSUR/Sage.
 */
public final class BaremeSituationMock {

    private BaremeSituationMock() {
    }

    public record Trio(BigDecimal production, BigDecimal encaisse, BigDecimal verse) {
    }

    public static Trio pour(String codeAgence, MoisSituation mois) {
        double f = FacteurAgenceVersementMock.pour(codeAgence);
        double m = multiplicateurMois(mois.mois());
        return new Trio(bd(9_200_000, f, m), bd(7_850_000, f, m), bd(6_400_000, f, m));
    }

    private static double multiplicateurMois(int mois) {
        return switch (mois) {
            case 6 -> 1.0;
            case 5 -> 0.95;
            case 4 -> 0.90;
            case 3 -> 0.86;
            default -> 1.0;
        };
    }

    private static BigDecimal bd(long base, double facteurAgence, double multMois) {
        return BigDecimal.valueOf(base)
                .multiply(BigDecimal.valueOf(facteurAgence))
                .multiply(BigDecimal.valueOf(multMois))
                .setScale(0, RoundingMode.HALF_UP);
    }
}
