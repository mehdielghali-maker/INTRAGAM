package dz.gam.poste.indicateursmock;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.versement.config.VersementProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Sème des chiffres FICTIFS distincts par agence (substitut du Cube Power BI). Réagit à
 * {@link AgencesDeclareesEvent} (démarrage et ajout via l'admin) : crée les mesures et la
 * situation mensuelle manquantes de chaque agence. Idempotent (n'écrase jamais une saisie).
 * Au démarrage, complète aussi les champs cumulés manquants sur TOUTES les lignes existantes
 * (migration des bases antérieures à l'ajout de l'écart cumulé).
 */
@Component
@Order(2) // après ProfilsSeeder (déclaration des agences)
public class IndicateursMockSeed implements ApplicationRunner {

    private final MesuresAgenceJpaRepository mesures;
    private final SituationMensuelleJpaRepository situations;
    private final VersementProperties versementProperties;

    public IndicateursMockSeed(MesuresAgenceJpaRepository mesures, SituationMensuelleJpaRepository situations,
                               VersementProperties versementProperties) {
        this.mesures = mesures;
        this.situations = situations;
        this.versementProperties = versementProperties;
    }

    @EventListener
    public void surAgencesDeclarees(AgencesDeclareesEvent evenement) {
        garantir(evenement.codesAgences());
    }

    /** Backfill global : complète les cumulés nuls de toutes les mesures déjà en base. */
    @Override
    public void run(ApplicationArguments args) {
        mesures.findAll().forEach(e -> {
            if (e.encaisseCumul == null || e.deposeCumul == null) {
                double f = facteur(e.codeAgence);
                e.encaisseCumul = bd(98_000_000, f);
                e.deposeCumul = bd(92_000_000, f);
                mesures.save(e);
            }
        });
    }

    public void garantir(List<String> codes) {
        List<String> mois = versementProperties.moisDisponibles();
        for (String code : codes) {
            double facteur = facteur(code);
            MesuresAgenceEntity existant = mesures.findByCodeAgence(code).orElse(null);
            if (existant == null) {
                mesures.save(mesuresParDefaut(code, facteur));
            } else if (existant.encaisseCumul == null || existant.deposeCumul == null) {
                // Backfill des champs cumulés ajoutés après coup (ne touche pas aux autres saisies).
                existant.encaisseCumul = bd(98_000_000, facteur);
                existant.deposeCumul = bd(92_000_000, facteur);
                mesures.save(existant);
            }
            for (String m : mois) {
                if (situations.findByCodeAgenceAndMois(code, m).isEmpty()) {
                    situations.save(situationParDefaut(code, m, facteur));
                }
            }
        }
    }

    /** Facteur déterministe par agence dans [0,40 ; 1,00] → chiffres distincts. */
    private static double facteur(String code) {
        return 0.40 + (Math.abs(code.hashCode()) % 61) / 100.0;
    }

    private MesuresAgenceEntity mesuresParDefaut(String code, double f) {
        MesuresAgenceEntity e = new MesuresAgenceEntity();
        e.codeAgence = code;
        e.caYtdN = bd(112_380_000, f);
        e.caYtdN1 = bd(104_200_000, f);
        e.caMoisN = bd(18_540_000, f);
        e.caMoisM1 = bd(16_980_000, f);
        e.productionMois = bd(18_540_000, f);
        e.encaisseMois = bd(17_200_000, f);
        e.deposeMois = bd(15_900_000, f);
        // Cumulés YTD (> mensuel) → écart cumulé à régulariser de la carte KPI.
        e.encaisseCumul = bd(98_000_000, f);
        e.deposeCumul = bd(92_000_000, f);
        e.echuNonEncaisse = bd(12_000_000, f);
        e.echuNonEncaisseM1 = bd(12_500_000, f);
        e.encaissementsLettres = bd(3_550_000, f);
        e.encaissementsLettresM1 = bd(3_680_000, f);
        e.sinistres12m = bd(68_400_000, f);
        e.primes12m = bd(100_000_000, f);
        e.sinistres12mN1 = bd(71_200_000, f);
        e.primes12mN1 = bd(100_000_000, f);
        e.contratsActifs = (int) Math.round(3247 * f);
        e.contratsActifsVariation = (int) Math.round(58 * f);
        return e;
    }

    private SituationMensuelleEntity situationParDefaut(String code, String mois, double f) {
        double m = multiplicateurMois(mois);
        SituationMensuelleEntity s = new SituationMensuelleEntity();
        s.codeAgence = code;
        s.mois = mois;
        s.productionEmise = bd(9_200_000, f * m);
        s.encaisse = bd(7_850_000, f * m);
        s.verse = bd(6_400_000, f * m);
        return s;
    }

    private static double multiplicateurMois(String mois) {
        int m = Integer.parseInt(mois.substring(5, 7));
        return switch (m) {
            case 6 -> 1.0;
            case 5 -> 0.95;
            case 4 -> 0.90;
            case 3 -> 0.86;
            default -> 1.0;
        };
    }

    private static BigDecimal bd(long base, double facteur) {
        return BigDecimal.valueOf(base).multiply(BigDecimal.valueOf(facteur)).setScale(0, RoundingMode.HALF_UP);
    }
}
