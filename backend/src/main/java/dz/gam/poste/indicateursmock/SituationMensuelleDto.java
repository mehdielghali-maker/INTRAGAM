package dz.gam.poste.indicateursmock;

import java.math.BigDecimal;

/** Vue/édition de la situation mensuelle d'une agence (substitut du Cube Power BI). DA. */
public record SituationMensuelleDto(
        String codeAgence,
        String mois,
        BigDecimal productionEmise,
        BigDecimal encaisse,
        BigDecimal verse) {

    static SituationMensuelleDto de(SituationMensuelleEntity e) {
        return new SituationMensuelleDto(e.codeAgence, e.mois, e.productionEmise, e.encaisse, e.verse);
    }
}
