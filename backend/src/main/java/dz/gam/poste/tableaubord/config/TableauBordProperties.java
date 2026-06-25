package dz.gam.poste.tableaubord.config;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.PerimetreSP;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Paramètres MÉTIER du tableau de bord, exposés en configuration — jamais figés en dur
 * (préfixe {@code poste.tableau-bord}). À caler avec la DG / la DFC.
 *
 * @param seuilsEcart   bornes du code couleur de l'écart « à régulariser » (écart / CA annuel)
 * @param sp            paramètres du ratio S/P (périmètre du numérateur)
 * @param periodeDefaut période affichée par défaut
 */
@ConfigurationProperties(prefix = "poste.tableau-bord")
public record TableauBordProperties(SeuilsEcart seuilsEcart, Sp sp, Periode periodeDefaut) {

    /**
     * Bornes (en %) du code couleur écart / CA annuel extrapolé :
     * &lt; {@code correctMax} → vert (Correct) ; &lt; {@code modereMax} → orange (Modéré) ;
     * ≤ {@code critiqueMax} → rouge (Critique) ; au-delà → noir (Danger).
     */
    public record SeuilsEcart(BigDecimal correctMax, BigDecimal modereMax, BigDecimal critiqueMax) {
    }

    public record Sp(PerimetreSP perimetreNumerateur) {
    }
}
