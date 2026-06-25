package dz.gam.poste.tableaubord.adapter.out.sagemock;

import dz.gam.poste.tableaubord.adapter.out.proassurmock.FacteurAgenceMock;
import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursSagePort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresSage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Adapter MOCK du système de référence Sage (comptabilité) pour les mesures « cash ».
 * Valeurs fictives cohérentes avec la maquette, pondérées par le facteur d'agence active
 * (cohérent avec le mock PROASSUR). À remplacer par l'API réelle Sage.
 */
@Component
public class SageMockIndicateursAdapter implements IndicateursSagePort {

    @Override
    public MesuresSage mesurer(String codeAgence, Periode periode) {
        double f = FacteurAgenceMock.pour(codeAgence);
        if (periode == Periode.YTD) {
            return new MesuresSage(
                    bd(15_900_000, f),   // déposé en banque (mois)
                    bd(3_550_000, f),    // encaissements lettrés
                    bd(3_680_000, f));   // encaissements lettrés M-1
        }
        // Mois courant
        return new MesuresSage(bd(16_400_000, f), bd(2_900_000, f), bd(3_000_000, f));
    }

    private static BigDecimal bd(long valeur, double facteur) {
        return BigDecimal.valueOf(valeur).multiply(BigDecimal.valueOf(facteur)).setScale(0, RoundingMode.HALF_UP);
    }
}
