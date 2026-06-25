package dz.gam.poste.versement.adapter.out.sagemock;

import dz.gam.poste.versement.adapter.out.proassurmock.BaremeSituationMock;
import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.port.out.VersementsBanquePort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter MOCK de Sage : montant déjà versé en banque d'un mois pour une agence
 * (cohérent avec le barème PROASSUR du même mock). À remplacer par l'API réelle Sage.
 */
@Component
public class VersementsBanqueMockAdapter implements VersementsBanquePort {

    @Override
    public BigDecimal montantDejaVerse(String codeAgence, MoisSituation mois) {
        return BaremeSituationMock.pour(codeAgence, mois).verse();
    }
}
