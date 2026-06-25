package dz.gam.poste.shared.regularisation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcul UNIQUE de l'écart « à régulariser » = Encaissé (PROASSUR) − Versé en banque (Sage).
 *
 * <p>Source de vérité partagée entre l'accueil (KPI « Écart à régulariser ») et la fonction
 * « Versement bancaire » (« Reste à régulariser ») : même définition, même formule, pas de
 * divergence possible (ADR 0005). Le seuil d'alerte (montant OU pourcentage) est passé par
 * l'appelant depuis sa configuration.
 */
public final class EcartRegularisation {

    private EcartRegularisation() {
    }

    public static Resultat calculer(BigDecimal encaisse, BigDecimal verse,
                                    BigDecimal seuilMontant, BigDecimal seuilPourcentage) {
        BigDecimal ecart = encaisse.subtract(verse);
        BigDecimal pourcentage = encaisse.signum() == 0
                ? BigDecimal.ZERO
                : ecart.divide(encaisse, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        boolean aRegulariser = ecart.compareTo(seuilMontant) >= 0
                || pourcentage.compareTo(seuilPourcentage) >= 0;
        return new Resultat(ecart, pourcentage, aRegulariser);
    }

    /** Résultat du calcul : écart (DA), pourcentage (écart/encaissé) et drapeau d'alerte. */
    public record Resultat(BigDecimal valeur, BigDecimal pourcentage, boolean aRegulariser) {
    }
}
