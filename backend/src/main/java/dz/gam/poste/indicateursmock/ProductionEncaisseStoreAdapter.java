package dz.gam.poste.indicateursmock;

import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.port.out.ProductionEncaissePort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter PROASSUR (situation mensuelle du versement) lisant le store éditable (substitut
 * du Cube Power BI). Repli sur la photo mensuelle de l'accueil si le mois n'est pas saisi.
 */
@Component
public class ProductionEncaisseStoreAdapter implements ProductionEncaissePort {

    private final SituationMensuelleJpaRepository situations;
    private final MesuresAgenceJpaRepository mesures;

    public ProductionEncaisseStoreAdapter(SituationMensuelleJpaRepository situations,
                                          MesuresAgenceJpaRepository mesures) {
        this.situations = situations;
        this.mesures = mesures;
    }

    @Override
    public SituationProduction productionEtEncaisse(String codeAgence, MoisSituation mois) {
        return situations.findByCodeAgenceAndMois(codeAgence, mois.valeur())
                .map(s -> new SituationProduction(s.productionEmise, s.encaisse))
                .orElseGet(() -> mesures.findByCodeAgence(codeAgence)
                        .map(e -> new SituationProduction(e.productionMois, e.encaisseMois))
                        .orElse(new SituationProduction(BigDecimal.ZERO, BigDecimal.ZERO)));
    }
}
