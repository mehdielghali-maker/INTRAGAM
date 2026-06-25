package dz.gam.poste.indicateursmock;

import java.math.BigDecimal;

/** Vue/édition des mesures KPI d'une agence (substitut du Cube Power BI). Montants en DA. */
public record MesuresAgenceDto(
        String codeAgence,
        BigDecimal caYtdN,
        BigDecimal caYtdN1,
        BigDecimal caGlissant12m,
        BigDecimal caMoisN,
        BigDecimal caMoisM1,
        BigDecimal productionMois,
        BigDecimal encaisseMois,
        BigDecimal deposeMois,
        BigDecimal encaisseCumul,
        BigDecimal deposeCumul,
        BigDecimal echuNonEncaisse,
        BigDecimal echuNonEncaisseM1,
        BigDecimal encaissementsLettres,
        BigDecimal encaissementsLettresM1,
        BigDecimal sinistres12m,
        BigDecimal primes12m,
        BigDecimal sinistres12mN1,
        BigDecimal primes12mN1,
        int contratsActifs,
        int contratsActifsVariation) {

    static MesuresAgenceDto de(MesuresAgenceEntity e) {
        return new MesuresAgenceDto(e.codeAgence, e.caYtdN, e.caYtdN1, e.caGlissant12m, e.caMoisN, e.caMoisM1,
                e.productionMois, e.encaisseMois, e.deposeMois, e.encaisseCumul, e.deposeCumul,
                e.echuNonEncaisse, e.echuNonEncaisseM1,
                e.encaissementsLettres, e.encaissementsLettresM1, e.sinistres12m, e.primes12m,
                e.sinistres12mN1, e.primes12mN1, e.contratsActifs, e.contratsActifsVariation);
    }
}
