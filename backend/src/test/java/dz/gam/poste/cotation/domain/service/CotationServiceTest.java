package dz.gam.poste.cotation.domain.service;

import dz.gam.poste.cotation.config.CotationProperties;
import dz.gam.poste.cotation.domain.event.DemandeCotationEmiseEvent;
import dz.gam.poste.cotation.domain.model.ContexteCotation;
import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.IdentiteAgence;
import dz.gam.poste.cotation.domain.model.PieceJointe;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import dz.gam.poste.cotation.domain.port.in.CreerDemandeCommand;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
import dz.gam.poste.cotation.domain.port.out.IdentiteAgencePort;
import dz.gam.poste.cotation.domain.port.out.PublicationCotationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CotationServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-23T09:00:00Z"), ZoneOffset.UTC);

    private FauxDemandeCotationRepository repository;
    private List<DemandeCotationEmiseEvent> publies;
    private CotationService service;

    @BeforeEach
    void init() {
        repository = new FauxDemandeCotationRepository();
        publies = new ArrayList<>();
        PublicationCotationPort publication = publies::add;
        IdentiteAgencePort identite = () ->
                new IdentiteAgence("M. Saïdi", "16.I.Gharbi", "Agence Bab Ezzouar", "DR Alger-Est");
        // GED mock : renvoie une référence, ne stocke pas le binaire.
        service = new CotationService(repository, publication, identite,
                nom -> new PieceJointe(nom, "GED-TEST"), proprietes(), HORLOGE);
    }

    private CotationProperties proprietes() {
        return new CotationProperties("DC", 412, List.of("Auto — flotte", "RC entreprise"),
                Map.of(StatutCotation.BROUILLON, "Brouillon", StatutCotation.ENVOYEE, "Envoyée",
                        StatutCotation.EN_COURS, "En cours", StatutCotation.A_FINALISER, "À finaliser",
                        StatutCotation.AFFAIRE_GAGNEE, "Affaire gagnée", StatutCotation.SANS_SUITE, "Sans suite"),
                new CotationProperties.Mock(0, 0),
                new CotationProperties.Identite("M. Saïdi", "16.I.Gharbi", "Agence Bab Ezzouar", "DR Alger-Est"));
    }

    private CreerDemandeCommand commande() {
        return new CreerDemandeCommand("Auto — flotte", "SARL Yassir", null, "Devis VTC",
                List.of("kbis.pdf"));
    }

    @Test
    void envoyer_genere_la_reference_publie_et_prefremplit_l_agence_depuis_le_sso() {
        DemandeCotation d = service.envoyer(commande());

        assertThat(d.statut()).isEqualTo(StatutCotation.ENVOYEE);
        assertThat(d.reference().valeur()).isEqualTo("DC-2026-0413"); // seed 412 + 1
        assertThat(d.codeAgence()).isEqualTo("16.I.Gharbi");          // depuis l'identité SSO, pas la requête
        assertThat(d.directionRegionale()).isEqualTo("DR Alger-Est");
        assertThat(d.createur()).isEqualTo("M. Saïdi");
        assertThat(d.piecesJointes()).singleElement()
                .satisfies(p -> assertThat(p.gedId()).startsWith("GED-"));
        assertThat(publies).hasSize(1);
        assertThat(d.souscripteur()).isNull(); // « Non affecté » tant que pas pris en charge
    }

    @Test
    void brouillon_ne_publie_pas_et_n_a_pas_de_reference() {
        DemandeCotation d = service.enregistrerBrouillon(commande());
        assertThat(d.statut()).isEqualTo(StatutCotation.BROUILLON);
        assertThat(d.reference()).isNull();
        assertThat(publies).isEmpty();
    }

    @Test
    void boucle_des_retours_central_met_a_jour_la_demande() {
        String ref = service.envoyer(commande()).reference().valeur();

        service.prendreEnCharge(ref, "K. Bensalem");          // source BPM
        service.finaliser(ref, "PR-88231", "DV-88231");       // source PROASSUR
        DemandeCotation apresFinalisation = service.lister(FiltreDemande.aucun()).get(0);
        assertThat(apresFinalisation.statut()).isEqualTo(StatutCotation.A_FINALISER);
        assertThat(apresFinalisation.souscripteur().nom()).isEqualTo("K. Bensalem");
        assertThat(apresFinalisation.numeroProposition()).isEqualTo("PR-88231");

        service.marquerAffaireGagnee(ref, "P-10044");
        assertThat(service.lister(FiltreDemande.aucun()).get(0).statut()).isEqualTo(StatutCotation.AFFAIRE_GAGNEE);
    }

    @Test
    void compter_actives_ignore_brouillons_et_terminaux() {
        service.envoyer(commande());                          // ENVOYEE (active)
        service.enregistrerBrouillon(commande());             // BROUILLON (non compté)
        assertThat(service.compterActives()).isEqualTo(1);
    }

    @Test
    void contexte_expose_l_identite_sso_les_branches_et_les_libelles() {
        ContexteCotation ctx = service.contexte();
        assertThat(ctx.identite().utilisateur()).isEqualTo("M. Saïdi");
        assertThat(ctx.identite().codeAgence()).isEqualTo("16.I.Gharbi");
        assertThat(ctx.branches()).contains("Auto — flotte");
        assertThat(ctx.libellesStatut()).containsEntry("A_FINALISER", "À finaliser");
    }

    @Test
    void marquer_sans_suite_par_identifiant() {
        DemandeCotation d = service.envoyer(commande());
        service.prendreEnCharge(d.reference().valeur(), "K. Bensalem");
        service.finaliser(d.reference().valeur(), "PR-1", "DV-1");

        DemandeCotation sansSuite = service.marquerSansSuite(d.id(), "Trop cher");
        assertThat(sansSuite.statut()).isEqualTo(StatutCotation.SANS_SUITE);
        assertThat(sansSuite.motifSansSuite()).isEqualTo("Trop cher");
    }
}
