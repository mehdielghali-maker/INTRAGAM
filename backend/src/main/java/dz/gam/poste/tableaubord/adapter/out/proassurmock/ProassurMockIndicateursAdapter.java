package dz.gam.poste.tableaubord.adapter.out.proassurmock;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.PerimetreSP;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursProassurPort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresProassur;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter MOCK du système de référence PROASSUR pour les mesures du tableau de bord.
 * Renvoie des valeurs fictives cohérentes avec la maquette validée. À remplacer par
 * l'API réelle PROASSUR sans toucher au domaine.
 *
 * <p>Deux jeux de valeurs (YTD / mois courant) pour que le sélecteur de période soit
 * réellement fonctionnel ; le S/P (12 mois glissants) est indépendant de ce choix.
 * Le périmètre du numérateur S/P fait varier la charge sinistres (paramètre de config).
 */
@Component
public class ProassurMockIndicateursAdapter implements IndicateursProassurPort {

    @Override
    public MesuresProassur mesurer(Periode periode, PerimetreSP perimetreSP) {
        boolean ytd = periode == Periode.YTD;

        BigDecimal sinistres12m = bd(perimetreSP == PerimetreSP.REGLES_SEULS ? 68_400_000 : 73_000_000);
        BigDecimal sinistres12mN1 = bd(perimetreSP == PerimetreSP.REGLES_SEULS ? 71_200_000 : 76_000_000);
        BigDecimal primes12m = bd(100_000_000);

        if (ytd) {
            return new MesuresProassur(
                    bd(112_380_000), bd(104_200_000),     // CA YTD N / N-1
                    bd(18_540_000), bd(16_980_000),        // CA mois N / M-1 même quantième
                    bd(18_540_000), bd(17_200_000),        // production / encaissé (mois)
                    bd(12_000_000), bd(12_500_000),        // échu non encaissé / M-1
                    sinistres12m, primes12m, sinistres12mN1, primes12m,
                    3247, 58);
        }
        // Mois courant
        return new MesuresProassur(
                bd(19_200_000), bd(18_100_000),
                bd(19_200_000), bd(18_100_000),
                bd(19_200_000), bd(17_900_000),
                bd(9_800_000), bd(9_600_000),
                sinistres12m, primes12m, sinistres12mN1, primes12m,
                3247, 58);
    }

    private static BigDecimal bd(long valeur) {
        return BigDecimal.valueOf(valeur);
    }
}
