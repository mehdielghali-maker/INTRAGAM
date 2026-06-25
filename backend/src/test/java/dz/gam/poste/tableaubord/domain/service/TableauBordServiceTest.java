package dz.gam.poste.tableaubord.domain.service;

import dz.gam.poste.tableaubord.config.TableauBordProperties;
import dz.gam.poste.tableaubord.domain.model.InfoAgence;
import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.PerimetreSP;
import dz.gam.poste.tableaubord.domain.model.TableauBord;
import dz.gam.poste.tableaubord.domain.model.UniteVariation;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgencePort;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursProassurPort;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursSagePort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresProassur;
import dz.gam.poste.tableaubord.domain.port.out.MesuresSage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TableauBordServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-23T09:00:00Z"), ZoneOffset.UTC);
    private static final InfoAgence AGENCE = new InfoAgence("Agence Saïd Hamdine", "02.1.S.BENZERGA");

    private final CompteursAgencePort compteurs = () -> new CompteursAgence(5, 7, 4, 3, 9, 2, 5, 2, 2);

    // Port PROASSUR factice : valeurs de la maquette, pondérées par un facteur propre à l'agence.
    private final IndicateursProassurPort proassur = (code, periode, perimetre) -> {
        double f = facteur(code);
        BigDecimal sin = bd(perimetre == PerimetreSP.REGLES_SEULS ? 68_400_000 : 73_000_000, f);
        BigDecimal sinN1 = bd(perimetre == PerimetreSP.REGLES_SEULS ? 71_200_000 : 76_000_000, f);
        BigDecimal primes = bd(100_000_000, f);
        return new MesuresProassur(
                bd(112_380_000, f), bd(104_200_000, f), bd(18_540_000, f), bd(16_980_000, f),
                bd(18_540_000, f), bd(17_200_000, f), bd(12_000_000, f), bd(12_500_000, f),
                sin, primes, sinN1, primes, (int) Math.round(3247 * f), 58);
    };

    private final IndicateursSagePort sage = (code, periode) -> {
        double f = facteur(code);
        return new MesuresSage(bd(15_900_000, f), bd(3_550_000, f), bd(3_680_000, f));
    };

    private static double facteur(String code) {
        return switch (code) {
            case "02.1.S.BENZERGA" -> 1.0;
            case "02.7.Draria" -> 0.4;
            default -> 1.0;
        };
    }

    private TableauBordProperties properties(BigDecimal seuilMontant, BigDecimal seuilPct, PerimetreSP perimetre) {
        return new TableauBordProperties(
                new TableauBordProperties.SeuilEcartDepot(seuilMontant, seuilPct),
                new TableauBordProperties.Sp(perimetre),
                Periode.YTD);
    }

    private TableauBordService serviceAvecMocks(PerimetreSP perimetre) {
        return new TableauBordService(proassur, sage, compteurs,
                properties(BigDecimal.valueOf(1_000_000), BigDecimal.TEN, perimetre), HORLOGE);
    }

    @Test
    void ytd_produit_les_cinq_kpi_attendus() {
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD, AGENCE);

        assertThat(tb.caYtd().valeur()).isEqualByComparingTo("112380000");
        assertThat(tb.caYtd().variation().valeur()).isEqualByComparingTo("7.9");
        assertThat(tb.caMois().variation().valeur()).isEqualByComparingTo("9.2");
        assertThat(tb.creances().valeur()).isEqualByComparingTo("8450000");
        assertThat(tb.creances().variation().valeur()).isEqualByComparingTo("-4.2");
        assertThat(tb.sp().valeur()).isEqualByComparingTo("68.4");
        assertThat(tb.sp().variation().valeur()).isEqualByComparingTo("-2.8");
        assertThat(tb.sp().variation().unite()).isEqualTo(UniteVariation.POINTS);
        assertThat(tb.productionDepots().ecart()).isEqualByComparingTo("1300000"); // encaissé − versé
        assertThat(tb.contratsActifs()).isEqualTo(3247);
        assertThat(tb.agence().code()).isEqualTo("02.1.S.BENZERGA");
    }

    @Test
    void ecart_a_regulariser_encaisse_moins_verse_au_dessus_du_seuil_est_terra() {
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD, AGENCE);
        assertThat(tb.ecartDepot().valeur()).isEqualByComparingTo("1300000");
        assertThat(tb.ecartDepot().pourcentage()).isEqualByComparingTo("7.6");
        assertThat(tb.ecartDepot().aRegulariser()).isTrue();
    }

    @Test
    void ecart_de_depot_sous_le_seuil_est_vert() {
        IndicateursProassurPort prod = (code, periode, perimetre) -> new MesuresProassur(
                bd(100, 1), bd(100, 1), bd(100, 1), bd(100, 1),
                bd(1_000_000, 1), bd(1_000_000, 1), bd(5_000_000, 1), bd(5_200_000, 1),
                bd(50, 1), bd(100, 1), bd(50, 1), bd(100, 1), 3247, 58);
        IndicateursSagePort sg = (code, periode) -> new MesuresSage(bd(985_000, 1), BigDecimal.ZERO, BigDecimal.ZERO);
        TableauBordService service = new TableauBordService(prod, sg, compteurs,
                properties(BigDecimal.valueOf(1_000_000), BigDecimal.TEN, PerimetreSP.REGLES_SEULS), HORLOGE);

        TableauBord tb = service.consulter(Periode.YTD, AGENCE);
        assertThat(tb.ecartDepot().valeur()).isEqualByComparingTo("15000");
        assertThat(tb.ecartDepot().aRegulariser()).isFalse();
    }

    @Test
    void le_perimetre_sp_change_le_numerateur() {
        BigDecimal spRegles = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD, AGENCE).sp().valeur();
        BigDecimal spProvisions = serviceAvecMocks(PerimetreSP.CHARGE_AVEC_PROVISIONS).consulter(Periode.YTD, AGENCE).sp().valeur();

        assertThat(spRegles).isEqualByComparingTo("68.4");
        assertThat(spProvisions).isGreaterThan(spRegles);
    }

    @Test
    void le_changement_d_agence_modifie_les_montants() {
        InfoAgence draria = new InfoAgence("Agence Draria", "02.7.Draria");
        BigDecimal caBenzerga = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD, AGENCE).caYtd().valeur();
        BigDecimal caDraria = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD, draria).caYtd().valeur();

        assertThat(caDraria).isLessThan(caBenzerga);
        assertThat(caDraria).isEqualByComparingTo("44952000"); // 112 380 000 × 0,4
    }

    @Test
    void vue_consolidee_somme_les_agences_et_fournit_la_repartition() {
        InfoAgence draria = new InfoAgence("Agence Draria", "02.7.Draria");
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS)
                .consolider(Periode.YTD, List.of(AGENCE, draria),
                        new InfoAgence("Toutes mes agences (consolidé)", "CONSOLIDE"));

        assertThat(tb.consolide()).isTrue();
        assertThat(tb.agence().code()).isEqualTo("CONSOLIDE");
        assertThat(tb.caYtd().valeur()).isEqualByComparingTo("157332000"); // 112 380 000 × (1,0 + 0,4)
        assertThat(tb.repartition()).hasSize(2);
        assertThat(tb.repartition().get(0).code()).isEqualTo("02.1.S.BENZERGA");
        assertThat(tb.repartition().get(1).code()).isEqualTo("02.7.Draria");
    }

    @Test
    void periode_nulle_utilise_la_valeur_par_defaut() {
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(null, AGENCE);
        assertThat(tb.periodeLibelle()).contains("YTD");
        assertThat(tb.agence().code()).isEqualTo("02.1.S.BENZERGA");
    }

    private static BigDecimal bd(long valeur, double facteur) {
        return BigDecimal.valueOf(valeur).multiply(BigDecimal.valueOf(facteur)).setScale(0, RoundingMode.HALF_UP);
    }
}
