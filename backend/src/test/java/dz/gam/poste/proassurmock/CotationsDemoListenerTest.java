package dz.gam.poste.proassurmock;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.ReferenceDemande;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
import dz.gam.poste.cotation.domain.port.out.DemandeCotationRepository;
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

class CotationsDemoListenerTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-23T10:00:00Z"), ZoneOffset.UTC);

    private final FauxRepository repository = new FauxRepository();
    private final CotationsDemoListener listener = new CotationsDemoListener(repository, HORLOGE);

    @Test
    void seme_trois_demandes_par_agence_avec_references_deterministes() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(List.of("16.I.Gharbi", "31.O.Bir")));

        assertThat(repository.demandesDe("16.I.Gharbi")).hasSize(3);
        assertThat(repository.demandesDe("31.O.Bir")).hasSize(3);
        assertThat(repository.trouverParReference(new ReferenceDemande("DC-16.I.Gharbi-001"))).isPresent();
        assertThat(repository.trouverParReference(new ReferenceDemande("DC-16.I.Gharbi-002"))).isPresent();
        assertThat(repository.trouverParReference(new ReferenceDemande("DC-16.I.Gharbi-003"))).isPresent();
    }

    @Test
    void seme_des_statuts_varies() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(List.of("16.I.Gharbi")));

        assertThat(statut("DC-16.I.Gharbi-001")).isEqualTo(StatutCotation.ENVOYEE);
        assertThat(statut("DC-16.I.Gharbi-002")).isEqualTo(StatutCotation.EN_COURS);
        assertThat(statut("DC-16.I.Gharbi-003")).isEqualTo(StatutCotation.A_FINALISER);
    }

    @Test
    void la_demande_en_cours_porte_un_souscripteur() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(List.of("16.I.Gharbi")));

        DemandeCotation enCours = repository
                .trouverParReference(new ReferenceDemande("DC-16.I.Gharbi-002")).orElseThrow();
        assertThat(enCours.souscripteur()).isNotNull();
        assertThat(enCours.souscripteur().nom()).isNotBlank();
    }

    @Test
    void ne_publie_pas_d_evenement_de_bus() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(List.of("16.I.Gharbi")));

        DemandeCotation envoyee = repository
                .trouverParReference(new ReferenceDemande("DC-16.I.Gharbi-001")).orElseThrow();
        assertThat(envoyee.evenementsNonPublies()).isEmpty();
    }

    @Test
    void est_idempotent_un_second_event_ne_duplique_pas() {
        AgencesDeclareesEvent evenement = new AgencesDeclareesEvent(List.of("16.I.Gharbi"));

        listener.surAgencesDeclarees(evenement);
        listener.surAgencesDeclarees(evenement);

        assertThat(repository.demandesDe("16.I.Gharbi")).hasSize(3);
        assertThat(repository.lister(FiltreDemande.aucun())).hasSize(3);
    }

    private StatutCotation statut(String reference) {
        return repository.trouverParReference(new ReferenceDemande(reference)).orElseThrow().statut();
    }

    /** Double de test en mémoire du repository des demandes (aucune dépendance JPA). */
    private static final class FauxRepository implements DemandeCotationRepository {

        private final Map<UUID, DemandeCotation> parId = new LinkedHashMap<>();

        @Override
        public DemandeCotation enregistrer(DemandeCotation demande) {
            parId.put(demande.id(), demande);
            return demande;
        }

        @Override
        public Optional<DemandeCotation> trouverParId(UUID id) {
            return Optional.ofNullable(parId.get(id));
        }

        @Override
        public Optional<DemandeCotation> trouverParReference(ReferenceDemande reference) {
            return parId.values().stream()
                    .filter(d -> d.reference() != null && d.reference().equals(reference))
                    .findFirst();
        }

        @Override
        public List<DemandeCotation> lister(FiltreDemande filtre) {
            List<DemandeCotation> resultat = new ArrayList<>();
            for (DemandeCotation d : parId.values()) {
                if (filtre.statut().map(s -> s == d.statut()).orElse(true)) {
                    resultat.add(d);
                }
            }
            return resultat;
        }

        @Override
        public long compterReferencees() {
            return parId.values().stream().filter(d -> d.reference() != null).count();
        }

        List<DemandeCotation> demandesDe(String agence) {
            return parId.values().stream().filter(d -> agence.equals(d.codeAgence())).toList();
        }
    }
}
