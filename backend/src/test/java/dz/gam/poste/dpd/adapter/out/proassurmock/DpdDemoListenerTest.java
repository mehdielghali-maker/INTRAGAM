package dz.gam.poste.dpd.adapter.out.proassurmock;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.dpd.adapter.out.ged.GedDpdMockAdapter;
import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.ReferenceDpd;
import dz.gam.poste.dpd.domain.model.StatutDpd;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.dpd.domain.port.out.AccordSuiviRepository;
import dz.gam.poste.dpd.domain.port.out.DemandeDpdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DpdDemoListenerTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-23T09:00:00Z"), ZoneOffset.UTC);
    private static final List<String> AGENCES = List.of("16.I.Gharbi", "16.II.Hydra");

    private FauxDemandes demandes;
    private FauxAccords accords;
    private DpdDemoListener listener;

    @BeforeEach
    void init() {
        demandes = new FauxDemandes();
        accords = new FauxAccords();
        listener = new DpdDemoListener(demandes, accords, new GedDpdMockAdapter(),
                new ProassurDpdMockAdapter(), HORLOGE);
    }

    @Test
    void seme_3_demandes_par_agence_avec_statuts_varies() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(AGENCES));

        assertThat(demandes.toutes()).hasSize(3 * AGENCES.size());

        for (String agence : AGENCES) {
            assertThat(statut(agence, 1)).isEqualTo(StatutDpd.ENVOYEE);
            assertThat(statut(agence, 2)).isEqualTo(StatutDpd.EN_VALIDATION);
            assertThat(statut(agence, 3)).isEqualTo(StatutDpd.ACCORDEE);
        }
    }

    @Test
    void chaque_demande_porte_son_agence_et_sa_piece_rc() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(AGENCES));

        for (String agence : AGENCES) {
            DemandeDpd d = demande(agence, 1);
            assertThat(d.codeAgence()).isEqualTo(agence);
            assertThat(d.aRegistreDeCommerce()).isTrue(); // RC déposée avant l'envoi
            assertThat(d.souscription().branche()).isEqualTo("Auto — flotte");
        }
    }

    @Test
    void seme_le_suivi_d_accord_pour_les_demandes_accordees() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(AGENCES));

        for (String agence : AGENCES) {
            String codeAccord = "AC-DEMO-%s-%03d".formatted(agence, 3);
            Optional<AccordSuivi> suivi = accords.trouverParCodeAccord(codeAccord);
            assertThat(suivi).isPresent();
            assertThat(suivi.get().versions()).hasSize(1);
            assertThat(suivi.get().versionCourante().echeances()).isNotEmpty();
        }
        // Seules les 2 demandes ACCORDEE (1 par agence) ont un suivi d'accord.
        assertThat(accords.taille()).isEqualTo(AGENCES.size());
    }

    @Test
    void rejouer_l_evenement_ne_duplique_pas_les_donnees() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(AGENCES));
        int demandesApres1erPassage = demandes.toutes().size();
        int accordsApres1erPassage = accords.taille();

        // 2e publication (démarrage + enregistrement de profil) : idempotence.
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(AGENCES));

        assertThat(demandes.toutes()).hasSize(demandesApres1erPassage);
        assertThat(accords.taille()).isEqualTo(accordsApres1erPassage);
    }

    private StatutDpd statut(String agence, int n) {
        return demande(agence, n).statut();
    }

    private DemandeDpd demande(String agence, int n) {
        ReferenceDpd ref = new ReferenceDpd("DE-DEMO-%s-%03d".formatted(agence, n));
        return demandes.trouverParReference(ref).orElseThrow();
    }

    /** Faux repository de demandes en mémoire. */
    private static final class FauxDemandes implements DemandeDpdRepository {
        private final Map<UUID, DemandeDpd> parId = new LinkedHashMap<>();

        @Override
        public DemandeDpd enregistrer(DemandeDpd d) {
            parId.put(d.id(), d);
            return d;
        }

        @Override
        public Optional<DemandeDpd> trouverParId(UUID id) {
            return Optional.ofNullable(parId.get(id));
        }

        @Override
        public Optional<DemandeDpd> trouverParReference(ReferenceDpd reference) {
            return parId.values().stream()
                    .filter(d -> d.reference() != null && d.reference().equals(reference))
                    .findFirst();
        }

        @Override
        public List<DemandeDpd> lister(FiltreDpd filtre) {
            return new ArrayList<>(parId.values());
        }

        @Override
        public long compterReferencees() {
            return parId.values().stream().filter(d -> d.reference() != null).count();
        }

        List<DemandeDpd> toutes() {
            return new ArrayList<>(parId.values());
        }
    }

    /** Faux repository de suivi d'accords en mémoire. */
    private static final class FauxAccords implements AccordSuiviRepository {
        private final Map<String, AccordSuivi> parCode = new LinkedHashMap<>();

        @Override
        public AccordSuivi enregistrer(AccordSuivi a) {
            parCode.put(a.codeAccord(), a);
            return a;
        }

        @Override
        public Optional<AccordSuivi> trouverParCodeAccord(String codeAccord) {
            return Optional.ofNullable(parCode.get(codeAccord));
        }

        int taille() {
            return parCode.size();
        }
    }
}
