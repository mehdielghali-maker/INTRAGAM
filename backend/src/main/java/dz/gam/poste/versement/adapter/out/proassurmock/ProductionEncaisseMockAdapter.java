package dz.gam.poste.versement.adapter.out.proassurmock;

import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.port.out.ProductionEncaissePort;
import org.springframework.stereotype.Component;

/**
 * Adapter MOCK de PROASSUR : production émise et encaissé d'un mois pour une agence.
 * Valeurs fictives cohérentes avec la maquette. À remplacer par l'API réelle PROASSUR.
 */
@Component
public class ProductionEncaisseMockAdapter implements ProductionEncaissePort {

    @Override
    public SituationProduction productionEtEncaisse(String codeAgence, MoisSituation mois) {
        BaremeSituationMock.Trio t = BaremeSituationMock.pour(codeAgence, mois);
        return new SituationProduction(t.production(), t.encaisse());
    }
}
