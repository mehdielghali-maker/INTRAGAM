package dz.gam.poste.indicateursmock;

import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.port.out.VersementsBanquePort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter Sage (montant versé en banque du versement) lisant le store éditable (substitut
 * du Cube Power BI). Repli sur la photo mensuelle de l'accueil si le mois n'est pas saisi.
 */
@Component
public class VersementsBanqueStoreAdapter implements VersementsBanquePort {

    private final SituationMensuelleJpaRepository situations;
    private final MesuresAgenceJpaRepository mesures;

    public VersementsBanqueStoreAdapter(SituationMensuelleJpaRepository situations,
                                        MesuresAgenceJpaRepository mesures) {
        this.situations = situations;
        this.mesures = mesures;
    }

    @Override
    public BigDecimal montantDejaVerse(String codeAgence, MoisSituation mois) {
        return situations.findByCodeAgenceAndMois(codeAgence, mois.valeur())
                .map(s -> s.verse)
                .orElseGet(() -> mesures.findByCodeAgence(codeAgence)
                        .map(e -> e.deposeMois)
                        .orElse(BigDecimal.ZERO));
    }
}
