package dz.gam.poste.tableaubord.domain.service;

import dz.gam.poste.tableaubord.adapter.out.proassurmock.ProassurMockIndicateursAdapter;
import dz.gam.poste.tableaubord.adapter.out.sagemock.SageMockIndicateursAdapter;
import dz.gam.poste.tableaubord.config.TableauBordProperties;
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

    private final CompteursAgencePort compteurs = () -> new CompteursAgence(5, 7, 4, 3, 9, 2, 5, 2);

    private TableauBordProperties properties(BigDecimal seuilMontant, BigDecimal seuilPct, PerimetreSP perimetre) {
        return new TableauBordProperties(
                new TableauBordProperties.SeuilEcartDepot(seuilMontant, seuilPct),
                new TableauBordProperties.Sp(perimetre),
                Periode.YTD,
                new TableauBordProperties.Agence("Agence Bab Ezzouar", "16.I.Gharbi"));
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
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD);

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

        // Bloc production & dépôts cohérent avec l'écart
        assertThat(tb.productionDepots().ecart()).isEqualByComparingTo("2640000");
        assertThat(tb.contratsActifs()).isEqualTo(3247);
    }

    @Test
    void ecart_de_depot_au_dessus_du_seuil_est_a_regulariser_terra() {
        // 2 640 000 (14,2 %) dépasse le seuil 1 000 000 / 10 %
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD);

        assertThat(tb.ecartDepot().valeur()).isEqualByComparingTo("2640000");
        assertThat(tb.ecartDepot().pourcentage()).isEqualByComparingTo("14.2");
        assertThat(tb.ecartDepot().aRegulariser()).isTrue();
    }

    @Test
    void ecart_de_depot_sous_le_seuil_est_vert() {
        IndicateursProassurPort proassur = (periode, perimetre) -> mesuresProassur(BigDecimal.valueOf(1_000_000));
        IndicateursSagePort sage = periode -> new MesuresSage(
                BigDecimal.valueOf(985_000), BigDecimal.ZERO, BigDecimal.ZERO);
        TableauBordService service = new TableauBordService(proassur, sage, compteurs,
                properties(BigDecimal.valueOf(1_000_000), BigDecimal.TEN, PerimetreSP.REGLES_SEULS), HORLOGE);

        // écart = 15 000 (1,5 %) : sous le seuil en montant ET en %
        TableauBord tb = service.consulter(Periode.YTD);
        assertThat(tb.ecartDepot().valeur()).isEqualByComparingTo("15000");
        assertThat(tb.ecartDepot().aRegulariser()).isFalse();
    }

    @Test
    void le_perimetre_sp_change_le_numerateur() {
        BigDecimal spRegles = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(Periode.YTD).sp().valeur();
        BigDecimal spProvisions = serviceAvecMocks(PerimetreSP.CHARGE_AVEC_PROVISIONS).consulter(Periode.YTD).sp().valeur();

        assertThat(spRegles).isEqualByComparingTo("68.4");
        assertThat(spProvisions).isGreaterThan(spRegles);
    }

    @Test
    void periode_nulle_utilise_la_valeur_par_defaut() {
        TableauBord tb = serviceAvecMocks(PerimetreSP.REGLES_SEULS).consulter(null);
        assertThat(tb.periodeLibelle()).contains("YTD");
        assertThat(tb.agence().code()).isEqualTo("16.I.Gharbi");
    }

    private MesuresProassur mesuresProassur(BigDecimal production) {
        return new MesuresProassur(
                BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                production, production,
                BigDecimal.valueOf(5_000_000), BigDecimal.valueOf(5_200_000),
                BigDecimal.valueOf(50), BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.valueOf(100),
                3247, 58);
    }
}
