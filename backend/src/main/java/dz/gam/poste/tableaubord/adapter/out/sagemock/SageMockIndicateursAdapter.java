package dz.gam.poste.tableaubord.adapter.out.sagemock;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursSagePort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresSage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter MOCK du système de référence Sage (comptabilité) pour les mesures « cash ».
 * Valeurs fictives cohérentes avec la maquette. À remplacer par l'API réelle Sage.
 */
@Component
public class SageMockIndicateursAdapter implements IndicateursSagePort {

    @Override
    public MesuresSage mesurer(Periode periode) {
        if (periode == Periode.YTD) {
            return new MesuresSage(
                    bd(15_900_000),   // déposé en banque (mois)
                    bd(3_550_000),    // encaissements lettrés
                    bd(3_680_000));   // encaissements lettrés M-1
        }
        // Mois courant
        return new MesuresSage(bd(16_400_000), bd(2_900_000), bd(3_000_000));
    }

    private static BigDecimal bd(long valeur) {
        return BigDecimal.valueOf(valeur);
    }
}
