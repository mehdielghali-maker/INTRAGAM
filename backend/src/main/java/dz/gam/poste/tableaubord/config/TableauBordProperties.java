package dz.gam.poste.tableaubord.config;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.PerimetreSP;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Paramètres MÉTIER du tableau de bord, exposés en configuration — jamais figés en dur
 * (préfixe {@code poste.tableau-bord}). À caler avec la DG / la DFC.
 *
 * @param seuilEcartDepot seuil de déclenchement de l'alerte « écart à régulariser »
 * @param sp              paramètres du ratio S/P (périmètre du numérateur)
 * @param periodeDefaut   période affichée par défaut
 */
@ConfigurationProperties(prefix = "poste.tableau-bord")
public record TableauBordProperties(SeuilEcartDepot seuilEcartDepot, Sp sp, Periode periodeDefaut) {

    /** Seuil de l'écart de dépôt : alerte si le montant OU le pourcentage est dépassé. */
    public record SeuilEcartDepot(BigDecimal montant, BigDecimal pourcentage) {
    }

    public record Sp(PerimetreSP perimetreNumerateur) {
    }
}
