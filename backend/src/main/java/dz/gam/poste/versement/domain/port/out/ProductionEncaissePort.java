package dz.gam.poste.versement.domain.port.out;

import dz.gam.poste.versement.domain.model.MoisSituation;

import java.math.BigDecimal;

/**
 * Port de sortie vers PROASSUR : production émise et encaissé d'un mois pour une agence.
 * Mock en dev ; remplaçable par l'API réelle. Le poste lit, il ne recalcule pas.
 */
public interface ProductionEncaissePort {

    SituationProduction productionEtEncaisse(String codeAgence, MoisSituation mois);

    /** Mesures brutes PROASSUR du mois (DA). */
    record SituationProduction(BigDecimal productionEmise, BigDecimal encaisse) {
    }
}
