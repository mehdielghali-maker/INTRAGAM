package dz.gam.poste.tableaubord.domain.service;

import dz.gam.poste.tableaubord.config.TableauBordProperties;
import dz.gam.poste.tableaubord.domain.model.CarteEcart;
import dz.gam.poste.tableaubord.domain.model.CarteMontant;
import dz.gam.poste.tableaubord.domain.model.CarteRatio;
import dz.gam.poste.tableaubord.domain.model.CompteurAction;
import dz.gam.poste.tableaubord.domain.model.InfoAgence;
import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.ProductionDepots;
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
import dz.gam.poste.shared.regularisation.EcartRegularisation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
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
        CompteursAgence c = compteurs.compteurs();

        CarteMontant caYtd = new CarteMontant(
                p.caYtdN(), Variation.pourcentage(p.caYtdN(), p.caYtdN1(), true),
                p.caYtdN1(), "vs N‑1");

        CarteMontant caMois = new CarteMontant(
                p.caMoisN(), Variation.pourcentage(p.caMoisN(), p.caMoisM1MemeQuantieme(), true),
                p.caMoisM1MemeQuantieme(), "vs M‑1 même jour");

        // Écart « à régulariser » = Encaissé (PROASSUR) − Versé en banque (Sage).
        // Définition canonique unique, partagée avec la fonction « Versement bancaire ».
        CarteEcart ecartDepot = calculerEcartDepot(p.encaisseMois(), s.deposeMois());

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
                productionDepots);
    }

    @Override
    public CompteursAgence badges() {
        return compteurs.compteurs();
    }

    private CarteEcart calculerEcartDepot(BigDecimal encaisse, BigDecimal depose) {
        // Calcul partagé avec la fonction « Versement bancaire » (source unique, ADR 0005).
        TableauBordProperties.SeuilEcartDepot seuil = properties.seuilEcartDepot();
        EcartRegularisation.Resultat r = EcartRegularisation.calculer(
                encaisse, depose, seuil.montant(), seuil.pourcentage());
        return new CarteEcart(r.valeur(), r.pourcentage(), r.aRegulariser());
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
