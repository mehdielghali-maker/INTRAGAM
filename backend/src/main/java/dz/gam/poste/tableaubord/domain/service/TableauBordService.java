package dz.gam.poste.tableaubord.domain.service;

import dz.gam.poste.tableaubord.config.TableauBordProperties;
import dz.gam.poste.tableaubord.domain.model.CarteEcart;
import dz.gam.poste.tableaubord.domain.model.CarteMontant;
import dz.gam.poste.tableaubord.domain.model.NiveauEcart;
import dz.gam.poste.tableaubord.domain.model.CarteRatio;
import dz.gam.poste.tableaubord.domain.model.CompteurAction;
import dz.gam.poste.tableaubord.domain.model.InfoAgence;
import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.ProductionDepots;
import dz.gam.poste.tableaubord.domain.model.RepartitionAgence;
import dz.gam.poste.tableaubord.domain.model.TableauBord;
import dz.gam.poste.tableaubord.domain.model.Variation;
import dz.gam.poste.tableaubord.domain.port.in.ConsulterNavigationUseCase;
import dz.gam.poste.tableaubord.domain.port.in.ConsulterTableauBordUseCase;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgencePort;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursProassurPort;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursSagePort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresProassur;
import dz.gam.poste.tableaubord.domain.port.out.MesuresSage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service applicatif du tableau de bord. Java pur (aucune annotation framework) :
 * il lit les mesures brutes dans les systèmes de référence (via ports) et en dérive
 * les indicateurs de présentation. Il ne recalcule jamais une vérité comptable/technique.
 */
public class TableauBordService implements ConsulterTableauBordUseCase, ConsulterNavigationUseCase {

    private static final Locale FR = Locale.FRENCH;

    private final IndicateursProassurPort proassur;
    private final IndicateursSagePort sage;
    private final CompteursAgencePort compteurs;
    private final TableauBordProperties properties;
    private final Clock horloge;

    public TableauBordService(IndicateursProassurPort proassur, IndicateursSagePort sage,
                              CompteursAgencePort compteurs, TableauBordProperties properties, Clock horloge) {
        this.proassur = proassur;
        this.sage = sage;
        this.compteurs = compteurs;
        this.properties = properties;
        this.horloge = horloge;
    }

    @Override
    public TableauBord consulter(Periode periode, InfoAgence agence) {
        Periode effective = periode != null ? periode : properties.periodeDefaut();
        MesuresProassur p = proassur.mesurer(agence.code(), effective, properties.sp().perimetreNumerateur());
        MesuresSage s = sage.mesurer(agence.code(), effective);
        return construire(effective, agence, p, s, false, List.of());
    }

    @Override
    public TableauBord consolider(Periode periode, List<InfoAgence> agences, InfoAgence entete) {
        Periode effective = periode != null ? periode : properties.periodeDefaut();
        MesuresProassur pTotal = null;
        MesuresSage sTotal = null;
        List<RepartitionAgence> repartition = new ArrayList<>();
        for (InfoAgence agence : agences) {
            MesuresProassur p = proassur.mesurer(agence.code(), effective, properties.sp().perimetreNumerateur());
            MesuresSage s = sage.mesurer(agence.code(), effective);
            repartition.add(new RepartitionAgence(agence.code(), agence.nom(),
                    p.caYtdN(), p.encaisseMois(), s.deposeMois(),
                    p.encaisseMois().subtract(s.deposeMois())));
            pTotal = pTotal == null ? p : additionner(pTotal, p);
            sTotal = sTotal == null ? s : additionner(sTotal, s);
        }
        return construire(effective, entete, pTotal, sTotal, true, repartition);
    }

    @Override
    public CompteursAgence badges() {
        return compteurs.compteurs();
    }

    private TableauBord construire(Periode effective, InfoAgence agence, MesuresProassur p, MesuresSage s,
                                   boolean consolide, List<RepartitionAgence> repartition) {
        CompteursAgence c = compteurs.compteurs();

        CarteMontant caYtd = new CarteMontant(
                p.caYtdN(), Variation.pourcentage(p.caYtdN(), p.caYtdN1(), true),
                p.caYtdN1(), "vs N‑1");

        CarteMontant caMois = new CarteMontant(
                p.caMoisN(), Variation.pourcentage(p.caMoisN(), p.caMoisM1MemeQuantieme(), true),
                p.caMoisM1MemeQuantieme(), "vs M‑1 même jour");

        // Écart « à régulariser » = Encaissé (PROASSUR) − Versé en banque (Sage), en CUMULÉ (YTD).
        // C'est l'écart cumulé à régulariser (l'écart DU MOIS figure dans le bloc Production & dépôts).
        // Code couleur = écart / CA annuel extrapolé (cf. niveau()).
        CarteEcart ecartDepot = calculerEcartDepot(p.encaisseCumul(), s.deposeCumul(), p.caYtdN());

        BigDecimal creance = p.echuNonEncaisse().subtract(s.encaissementsLettres());
        BigDecimal creanceM1 = p.echuNonEncaisseM1().subtract(s.encaissementsLettresM1());
        CarteMontant creances = new CarteMontant(
                creance, Variation.pourcentage(creance, creanceM1, false),
                creanceM1, "échues non encaissées · vs M‑1");

        BigDecimal sp = ratio(p.sinistres12m(), p.primes12m());
        BigDecimal spN1 = ratio(p.sinistres12mN1(), p.primes12mN1());
        CarteRatio carteSp = new CarteRatio(sp, Variation.points(sp, spN1, true), spN1);

        ProductionDepots productionDepots = new ProductionDepots(
                p.productionMois(), p.encaisseMois(), s.deposeMois(),
                p.encaisseMois().subtract(s.deposeMois()));

        return new TableauBord(
                agence,
                libellePeriode(effective),
                libelleDate(),
                p.contratsActifs(),
                p.contratsActifsVariation(),
                caYtd, caMois, ecartDepot, creances, carteSp,
                coupDoeil(c),
                productionDepots,
                consolide,
                List.copyOf(repartition));
    }

    /** Somme des mesures PROASSUR de deux agences (vue consolidée). */
    private static MesuresProassur additionner(MesuresProassur a, MesuresProassur b) {
        return new MesuresProassur(
                a.caYtdN().add(b.caYtdN()), a.caYtdN1().add(b.caYtdN1()),
                a.caMoisN().add(b.caMoisN()), a.caMoisM1MemeQuantieme().add(b.caMoisM1MemeQuantieme()),
                a.productionMois().add(b.productionMois()), a.encaisseMois().add(b.encaisseMois()),
                a.encaisseCumul().add(b.encaisseCumul()),
                a.echuNonEncaisse().add(b.echuNonEncaisse()), a.echuNonEncaisseM1().add(b.echuNonEncaisseM1()),
                a.sinistres12m().add(b.sinistres12m()), a.primes12m().add(b.primes12m()),
                a.sinistres12mN1().add(b.sinistres12mN1()), a.primes12mN1().add(b.primes12mN1()),
                a.contratsActifs() + b.contratsActifs(), a.contratsActifsVariation() + b.contratsActifsVariation());
    }

    /** Somme des mesures Sage de deux agences (vue consolidée). */
    private static MesuresSage additionner(MesuresSage a, MesuresSage b) {
        return new MesuresSage(
                a.deposeMois().add(b.deposeMois()),
                a.deposeCumul().add(b.deposeCumul()),
                a.encaissementsLettres().add(b.encaissementsLettres()),
                a.encaissementsLettresM1().add(b.encaissementsLettresM1()));
    }

    private CarteEcart calculerEcartDepot(BigDecimal encaisseCumul, BigDecimal deposeCumul, BigDecimal caYtd) {
        // Écart cumulé = Encaissé − Versé (définition unique, ADR 0005).
        BigDecimal ecart = encaisseCumul.subtract(deposeCumul);
        // Sévérité = écart rapporté au CA ANNUEL extrapolé (YTD annualisé au prorata des jours).
        BigDecimal caAnnuel = extrapolerAnnuel(caYtd);
        BigDecimal ratio = (ecart.signum() <= 0 || caAnnuel.signum() == 0)
                ? BigDecimal.ZERO
                : ecart.divide(caAnnuel, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return new CarteEcart(ecart, ratio.setScale(1, RoundingMode.HALF_UP), niveau(ratio));
    }

    /** CA annuel extrapolé = CA YTD × (jours de l'année / jours écoulés). */
    private BigDecimal extrapolerAnnuel(BigDecimal caYtd) {
        LocalDate jour = LocalDate.now(horloge);
        return caYtd.multiply(BigDecimal.valueOf(jour.lengthOfYear()))
                .divide(BigDecimal.valueOf(jour.getDayOfYear()), 0, RoundingMode.HALF_UP);
    }

    /** Code couleur selon le ratio écart / CA annuel (bornes en config). */
    private NiveauEcart niveau(BigDecimal ratioPct) {
        TableauBordProperties.SeuilsEcart s = properties.seuilsEcart();
        if (ratioPct.compareTo(s.correctMax()) < 0) {
            return NiveauEcart.CORRECT;
        }
        if (ratioPct.compareTo(s.modereMax()) < 0) {
            return NiveauEcart.MODERE;
        }
        if (ratioPct.compareTo(s.critiqueMax()) <= 0) {
            return NiveauEcart.CRITIQUE;
        }
        return NiveauEcart.DANGER;
    }

    private List<CompteurAction> coupDoeil(CompteursAgence c) {
        // urgent (rouge) pour les compteurs d'action critiques, comme la maquette.
        return List.of(
                new CompteurAction("cheques", c.chequesEnAttente(), c.chequesEnAttente() > 0),
                new CompteurAction("attestations", c.attestations(), false),
                new CompteurAction("cotations", c.cotations(), false),
                new CompteurAction("echeanciers", c.echeanciersRisque(), c.echeanciersRisque() > 0),
                new CompteurAction("contentieux", c.contentieux(), c.contentieux() > 0));
    }

    private static BigDecimal ratio(BigDecimal numerateur, BigDecimal denominateur) {
        if (denominateur.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerateur.divide(denominateur, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
    }

    private String libelleDate() {
        LocalDate jour = LocalDate.now(horloge);
        String jourSemaine = capitaliser(jour.getDayOfWeek().getDisplayName(TextStyle.FULL, FR));
        String mois = jour.getMonth().getDisplayName(TextStyle.FULL, FR);
        return jourSemaine + " " + jour.getDayOfMonth() + " " + mois + " " + jour.getYear();
    }

    private String libellePeriode(Periode periode) {
        LocalDate jour = LocalDate.now(horloge);
        String moisAnnee = capitaliser(jour.getMonth().getDisplayName(TextStyle.FULL, FR)) + " " + jour.getYear();
        return moisAnnee + " · " + periode.libelle();
    }

    private static String capitaliser(String valeur) {
        if (valeur.isEmpty()) {
            return valeur;
        }
        return Character.toUpperCase(valeur.charAt(0)) + valeur.substring(1);
    }
}
