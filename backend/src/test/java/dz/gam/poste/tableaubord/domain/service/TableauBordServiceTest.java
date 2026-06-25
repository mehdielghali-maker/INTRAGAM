package dz.gam.poste.tableaubord.domain.service;

import dz.gam.poste.tableaubord.adapter.out.proassurmock.ProassurMockIndicateursAdapter;
import dz.gam.poste.tableaubord.adapter.out.sagemock.SageMockIndicateursAdapter;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TableauBordServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-23T09:00:00Z"), ZoneOffset.UTC);

    // Agence de référence de la maquette (facteur mock = 1,0 → chiffres bruts).
    private static final InfoAgence AGENCE = new InfoAgence("Agence Saïd Hamdine", "02.1.S.BENZERGA");

    private final CompteursAgencePort compteurs = () -> new CompteursAgence(5, 7, 4, 3, 9, 2, 5, 2);

    private TableauBordProperties properties(BigDecimal seuilMontant, BigDecimal seuilPct, PerimetreSP perimetre) {
        return new TableauBordProperties(
                new TableauBordProperties.SeuilEcartDepot(seuilMontant, seuilPct),
                new TableauBordProperties.Sp(perimetre),
                Periode.YTD);
    }

    private TableauBordService serviceAvecMocks(PerimetreSP perimetre) {
        return new TableauBordService(
                new ProassurMockIndicateursAdapter(),
                new SageMockIndicateursAdapter(),
                compteurs,
                properties(BigDecimal.valueOf(1_000_000), BigDecimal.TEN, perimetre),
                HORLOGE);
    }

    @Test
    void ytd_produit_les_cinq_kpi_attendus() {
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD, AGENCE);

        // 1. CA YTD : 112 380 000 vs 104 200 000 → +7,9 % (dérivé par la formule)
        assertThat(tb.caYtd().valeur()).isEqualByComparingTo("112380000");
        assertThat(tb.caYtd().variation().valeur()).isEqualByComparingTo("7.9");
        assertThat(tb.caYtd().variation().favorable()).isTrue();

        // 2. CA du mois : 18 540 000 vs 16 980 000 → +9,2 %
        assertThat(tb.caMois().variation().valeur()).isEqualByComparingTo("9.2");
        assertThat(tb.caMois().variation().favorable()).isTrue();

        // 4. Créances : (12 000 000 − 3 550 000) = 8 450 000 ; M-1 = 8 820 000 → −4,2 % (baisse favorable)
        assertThat(tb.creances().valeur()).isEqualByComparingTo("8450000");
        assertThat(tb.creances().variation().valeur()).isEqualByComparingTo("-4.2");
        assertThat(tb.creances().variation().favorable()).isTrue();

        // 5. S/P : 68 400 000 / 100 000 000 = 68,4 % ; N-1 = 71,2 % → −2,8 points (baisse favorable)
        assertThat(tb.sp().valeur()).isEqualByComparingTo("68.4");
        assertThat(tb.sp().variation().valeur()).isEqualByComparingTo("-2.8");
        assertThat(tb.sp().variation().unite()).isEqualTo(UniteVariation.POINTS);
        assertThat(tb.sp().variation().favorable()).isTrue();

        // Bloc production & dépôts : écart = encaissé (17 200 000) − versé (15 900 000) = 1 300 000
        assertThat(tb.productionDepots().ecart()).isEqualByComparingTo("1300000");
        assertThat(tb.contratsActifs()).isEqualTo(3247);
        assertThat(tb.agence().code()).isEqualTo("02.1.S.BENZERGA");
    }

    @Test
    void ecart_a_regulariser_encaisse_moins_verse_au_dessus_du_seuil_est_terra() {
        // Encaissé 17 200 000 − versé 15 900 000 = 1 300 000 (7,6 %) : dépasse le seuil en montant (≥ 1 000 000)
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD, AGENCE);

        assertThat(tb.ecartDepot().valeur()).isEqualByComparingTo("1300000");
        assertThat(tb.ecartDepot().pourcentage()).isEqualByComparingTo("7.6");
        assertThat(tb.ecartDepot().aRegulariser()).isTrue();
    }

    @Test
    void ecart_de_depot_sous_le_seuil_est_vert() {
        IndicateursProassurPort proassur = (agence, periode, perimetre) -> mesuresProassur(BigDecimal.valueOf(1_000_000));
        IndicateursSagePort sage = (agence, periode) -> new MesuresSage(
                BigDecimal.valueOf(985_000), BigDecimal.ZERO, BigDecimal.ZERO);
        TableauBordService service = new TableauBordService(proassur, sage, compteurs,
                properties(BigDecimal.valueOf(1_000_000), BigDecimal.TEN, PerimetreSP.REGLES_SEULS), HORLOGE);

        // écart = encaissé 1 000 000 − versé 985 000 = 15 000 (1,5 %) : sous le seuil en montant ET en %
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

        // Draria (facteur mock 0,4) produit un CA plus faible que l'agence de référence.
        assertThat(caDraria).isLessThan(caBenzerga);
        assertThat(caDraria).isEqualByComparingTo("44952000"); // 112 380 000 × 0,4
    }

    @Test
    void periode_nulle_utilise_la_valeur_par_defaut() {
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(null, AGENCE);
        assertThat(tb.periodeLibelle()).contains("YTD");
        assertThat(tb.agence().code()).isEqualTo("02.1.S.BENZERGA");
    }

    private MesuresProassur mesuresProassur(BigDecimal encaisse) {
        return new MesuresProassur(
                BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                encaisse, encaisse,
                BigDecimal.valueOf(5_000_000), BigDecimal.valueOf(5_200_000),
                BigDecimal.valueOf(50), BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.valueOf(100),
                3247, 58);
    }
}
