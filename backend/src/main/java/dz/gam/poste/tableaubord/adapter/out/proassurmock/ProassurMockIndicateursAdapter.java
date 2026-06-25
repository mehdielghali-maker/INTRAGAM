package dz.gam.poste.tableaubord.adapter.out.proassurmock;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.PerimetreSP;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursProassurPort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresProassur;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Adapter MOCK du système de référence PROASSUR pour les mesures du tableau de bord.
 * Renvoie des valeurs fictives cohérentes avec la maquette validée. À remplacer par
 * l'API réelle PROASSUR sans toucher au domaine.
 *
 * <p>Deux jeux de valeurs (YTD / mois courant) pour que le sélecteur de période soit
 * réellement fonctionnel ; le S/P (12 mois glissants) est indépendant de ce choix.
 * Les montants sont pondérés par un facteur propre à l'agence active, afin que le
 * changement d'agence (commutateur global) modifie réellement les chiffres affichés.
 */
@Component
public class ProassurMockIndicateursAdapter implements IndicateursProassurPort {

    @Override
    public MesuresProassur mesurer(String codeAgence, Periode periode, PerimetreSP perimetreSP) {
        boolean ytd = periode == Periode.YTD;
        double f = FacteurAgenceMock.pour(codeAgence);

        BigDecimal sinistres12m = bd(perimetreSP == PerimetreSP.REGLES_SEULS ? 68_400_000 : 73_000_000, f);
        BigDecimal sinistres12mN1 = bd(perimetreSP == PerimetreSP.REGLES_SEULS ? 71_200_000 : 76_000_000, f);
        BigDecimal primes12m = bd(100_000_000, f);

        if (ytd) {
            return new MesuresProassur(
                    bd(112_380_000, f), bd(104_200_000, f),   // CA YTD N / N-1
                    bd(18_540_000, f), bd(16_980_000, f),      // CA mois N / M-1 même quantième
                    bd(18_540_000, f), bd(17_200_000, f),      // production / encaissé (mois)
                    bd(12_000_000, f), bd(12_500_000, f),      // échu non encaissé / M-1
                    sinistres12m, primes12m, sinistres12mN1, primes12m,
                    contrats(3247, f), 58);
        }
        // Mois courant
        return new MesuresProassur(
                bd(19_200_000, f), bd(18_100_000, f),
                bd(19_200_000, f), bd(18_100_000, f),
                bd(19_200_000, f), bd(17_900_000, f),
                bd(9_800_000, f), bd(9_600_000, f),
                sinistres12m, primes12m, sinistres12mN1, primes12m,
                contrats(3247, f), 58);
    }

    private static BigDecimal bd(long valeur, double facteur) {
        return BigDecimal.valueOf(valeur).multiply(BigDecimal.valueOf(facteur)).setScale(0, RoundingMode.HALF_UP);
    }

    private static int contrats(int valeur, double facteur) {
        return (int) Math.round(valeur * facteur);
    }
}
