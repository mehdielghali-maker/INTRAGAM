package dz.gam.poste.indicateursmock;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.PerimetreSP;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursProassurPort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresProassur;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter PROASSUR (KPI accueil) lisant les mesures éditables du store (substitut du Cube
 * Power BI). À remplacer par l'adapter Power BI réel sans toucher au domaine.
 */
@Component
public class ProassurIndicateursStoreAdapter implements IndicateursProassurPort {

    /** Charge sinistres majorée quand le périmètre S/P inclut les provisions. */
    private static final BigDecimal FACTEUR_PROVISIONS = BigDecimal.valueOf(1.07);

    private final MesuresAgenceJpaRepository repository;

    public ProassurIndicateursStoreAdapter(MesuresAgenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public MesuresProassur mesurer(String codeAgence, Periode periode, PerimetreSP perimetreSP) {
        MesuresAgenceEntity e = repository.findByCodeAgence(codeAgence).orElse(null);
        if (e == null) {
            BigDecimal z = BigDecimal.ZERO;
            return new MesuresProassur(z, z, z, z, z, z, z, z, z, z, z, BigDecimal.ONE, z, BigDecimal.ONE, 0, 0);
        }
        BigDecimal sinistres = e.sinistres12m;
        BigDecimal sinistresN1 = e.sinistres12mN1;
        if (perimetreSP == PerimetreSP.CHARGE_AVEC_PROVISIONS) {
            sinistres = sinistres.multiply(FACTEUR_PROVISIONS);
            sinistresN1 = sinistresN1.multiply(FACTEUR_PROVISIONS);
        }
        // Replis si valeurs absentes (anciennes lignes avant backfill).
        BigDecimal encaisseCumul = e.encaisseCumul != null ? e.encaisseCumul : e.encaisseMois;
        BigDecimal caGlissant12m = e.caGlissant12m != null ? e.caGlissant12m : e.caYtdN;
        return new MesuresProassur(
                e.caYtdN, e.caYtdN1, caGlissant12m, e.caMoisN, e.caMoisM1, e.productionMois, e.encaisseMois,
                encaisseCumul, e.echuNonEncaisse, e.echuNonEncaisseM1, sinistres, e.primes12m, sinistresN1,
                e.primes12mN1, e.contratsActifs, e.contratsActifsVariation);
    }
}
