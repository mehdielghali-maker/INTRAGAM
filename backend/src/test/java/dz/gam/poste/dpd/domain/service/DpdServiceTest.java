package dz.gam.poste.dpd.domain.service;

import dz.gam.poste.dpd.adapter.out.bpmmock.BpmDpdMockAdapter;
import dz.gam.poste.dpd.adapter.out.ged.GedDpdMockAdapter;
import dz.gam.poste.dpd.adapter.out.proassurmock.ProassurDpdMockAdapter;
import dz.gam.poste.dpd.config.DpdProperties;
import dz.gam.poste.dpd.domain.event.DemandePaiementDiffereEmiseEvent;
import dz.gam.poste.dpd.domain.event.EcheancierDpdMisAJourEvent;
import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.DpdExceptions;
import dz.gam.poste.dpd.domain.model.IdentiteDpd;
import dz.gam.poste.dpd.domain.model.StatutDpd;
import dz.gam.poste.dpd.domain.model.TypePersonne;
import dz.gam.poste.dpd.domain.port.in.CreerDpdCommand;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.dpd.domain.port.out.IdentiteDpdPort;
import dz.gam.poste.dpd.domain.port.out.PublicationDpdPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DpdServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-23T09:00:00Z"), ZoneOffset.UTC);

    private FauxDpdRepositories.Demandes demandes;
    private FauxDpdRepositories.Accords accords;
    private List<DemandePaiementDiffereEmiseEvent> emises;
    private List<EcheancierDpdMisAJourEvent> majs;
    private DpdService service;

    @BeforeEach
    void init() {
        demandes = new FauxDpdRepositories.Demandes();
        accords = new FauxDpdRepositories.Accords();
        emises = new ArrayList<>();
        majs = new ArrayList<>();
        PublicationDpdPort publication = new PublicationDpdPort() {
            @Override
            public void publier(DemandePaiementDiffereEmiseEvent e) {
                emises.add(e);
            }

            @Override
            public void publier(EcheancierDpdMisAJourEvent e) {
                majs.add(e);
            }
        };
        IdentiteDpdPort identite = () -> new IdentiteDpd("M. Saïdi", "16.I.Gharbi", "ag@gam.dz",
                "Agence Bab Ezzouar", "DR Alger-Est", "dr@gam.dz");
        service = new DpdService(demandes, accords, new ProassurDpdMockAdapter(), new BpmDpdMockAdapter(),
                new GedDpdMockAdapter(), identite, publication, proprietes(), HORLOGE);
    }

    private DpdProperties proprietes() {
        return new DpdProperties("DE", 231,
                Map.of(StatutDpd.BROUILLON, "Brouillon", StatutDpd.ENVOYEE, "Envoyée",
                        StatutDpd.EN_VALIDATION, "En validation", StatutDpd.ACCORDEE, "Accordée",
                        StatutDpd.REFUSEE, "Refusée"),
                "DR", new DpdProperties.Mock(0, 0),
                new DpdProperties.Identite("M. Saïdi", "16.I.Gharbi", "ag@gam.dz", "Agence Bab Ezzouar",
                        "DR Alger-Est", "dr@gam.dz"));
    }

    private CreerDpdCommand commande(List<String> rc) {
        return new CreerDpdCommand("PR-88231", false, "DPD 6 mois", "SARL Méditerranée", "SARL Méditerranée",
                "021", "RC123", TypePersonne.MORALE, false, "Alger", rc, List.of("annexe.pdf"));
    }

    @Test
    void prefill_trouve_et_introuvable() {
        assertThat(service.prefill("PR-88231")).isPresent();
        assertThat(service.prefill("INCONNU")).isEmpty();
    }

    @Test
    void envoyer_exige_le_rc() {
        assertThatThrownBy(() -> service.envoyer(commande(List.of())))
                .isInstanceOf(DpdExceptions.RcManquant.class);
        assertThat(emises).isEmpty();
    }

    @Test
    void envoyer_avec_rc_genere_reference_et_publie() {
        DemandeDpd d = service.envoyer(commande(List.of("rc.pdf")));
        assertThat(d.statut()).isEqualTo(StatutDpd.ENVOYEE);
        assertThat(d.reference().valeur()).isEqualTo("DE-2026-0232"); // seed 231 + 1
        assertThat(d.codeAgence()).isEqualTo("16.I.Gharbi");          // depuis le SSO
        assertThat(d.aRegistreDeCommerce()).isTrue();
        assertThat(emises).hasSize(1);
    }

    @Test
    void retours_central_jusqu_a_accordee_creent_le_suivi_versionne() {
        String ref = service.envoyer(commande(List.of("rc.pdf"))).reference().valeur();
        service.mettreEnValidation(ref);                 // BPM → validateur
        DemandeDpd enValidation = service.lister(FiltreDpd.aucun()).get(0);
        assertThat(enValidation.statut()).isEqualTo(StatutDpd.EN_VALIDATION);
        assertThat(enValidation.validateur()).isNotNull();

        service.accorder(ref, "AC-0232");                // PROASSUR → échéancier v1
        AccordSuivi suivi = accords.trouverParCodeAccord("AC-0232").orElseThrow();
        assertThat(suivi.versions()).hasSize(1);
        assertThat(suivi.versionCourante().total()).isEqualByComparingTo("1240000");
    }

    @Test
    void synchroniser_reflete_les_sous_totaux_et_emet_l_evenement() {
        AccordSuivi suivi = service.synchroniser("AC-9001");

        // 8 × 155 000 : 5 réglées (775 000), 3 restantes (465 000)
        assertThat(suivi.versionCourante().total()).isEqualByComparingTo("1240000");
        assertThat(suivi.versionCourante().nbReglees()).isEqualTo(5);
        assertThat(suivi.versionCourante().montantRegle()).isEqualByComparingTo("775000");
        assertThat(suivi.versionCourante().nbRestantes()).isEqualTo(3);
        assertThat(suivi.versionCourante().montantRestant()).isEqualByComparingTo("465000");
        assertThat(majs).hasSize(1);
    }

    @Test
    void synchroniser_historise_une_nouvelle_version() {
        service.synchroniser("AC-9001");                 // v1
        AccordSuivi suivi = service.synchroniser("AC-9001"); // v2 (renégociation)

        assertThat(suivi.versions()).hasSize(2);
        assertThat(suivi.versionCourante().version()).isEqualTo(2);
        assertThat(majs).hasSize(2);
    }

    @Test
    void accord_introuvable_sur_synchronisation() {
        assertThatThrownBy(() -> service.synchroniser("INCONNU"))
                .isInstanceOf(DpdExceptions.AccordIntrouvable.class);
    }
}
