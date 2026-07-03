package dz.gam.poste.versement.adapter.in.messaging;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que le seed de démo des versements crée 2 entrées DÉPOSÉ par agence, avec des
 * références DÉTERMINISTES ({@code VB-DEMO-{agence}-00n}), qu'il est idempotent (le rejeu de
 * l'événement ne duplique rien — garde par référence) et qu'il ne publie RIEN (agrégat vidé de
 * ses événements avant persistance : les démos restent DÉPOSÉES, sans boucle BPM).
 */
class VersementsDemoListenerTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-29T09:00:00Z"), ZoneOffset.UTC);
    private static final String AGENCE_A = "02.1.S.BENZERGA";
    private static final String AGENCE_B = "16.4.D.ALGER";

    private final FauxRepository repository = new FauxRepository();
    private final VersementsDemoListener listener = new VersementsDemoListener(repository, HORLOGE);

    @Test
    void seme_deux_versements_deposes_par_agence_avec_les_valeurs_attendues() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(List.of(AGENCE_A, AGENCE_B)));

        List<Versement> agenceA = repository.parAgence(AGENCE_A);
        assertThat(agenceA).hasSize(2);
        assertThat(agenceA).allSatisfy(v -> {
            assertThat(v.statut()).isEqualTo(StatutVersement.DEPOSE);
            assertThat(v.createur()).isEqualTo("Démo");
            assertThat(v.pieces()).hasSize(1);
            assertThat(v.commentaire()).isEqualTo("Versement de démo");
            // Aucun événement en attente : rien ne doit partir sur le bus pour une donnée de démo.
            assertThat(v.evenementsNonPublies()).isEmpty();
        });
        assertThat(agenceA).extracting(Versement::reference)
                .containsExactlyInAnyOrder("VB-DEMO-" + AGENCE_A + "-001", "VB-DEMO-" + AGENCE_A + "-002");
        assertThat(agenceA).extracting(v -> v.mois().valeur())
                .containsExactlyInAnyOrder("2026-05", "2026-04");
        assertThat(agenceA).extracting(Versement::banque)
                .containsExactlyInAnyOrder("BNA", "BEA");
        assertThat(agenceA).extracting(v -> v.montantVerse().toPlainString())
                .containsExactlyInAnyOrder("1850000", "1620000");

        // La 2e agence est semée elle aussi (2 entrées).
        assertThat(repository.parAgence(AGENCE_B)).hasSize(2);
    }

    @Test
    void est_idempotent_un_second_evenement_ne_duplique_pas() {
        AgencesDeclareesEvent evenement = new AgencesDeclareesEvent(List.of(AGENCE_A));

        listener.surAgencesDeclarees(evenement);
        listener.surAgencesDeclarees(evenement); // rejeu (démarrage + enregistrement de profil)

        assertThat(repository.parAgence(AGENCE_A)).hasSize(2);
        assertThat(repository.total()).isEqualTo(2);
    }

    @Test
    void un_echec_sur_une_agence_ne_bloque_pas_les_suivantes() {
        // Repository qui refuse la 1re agence : le listener logge et continue avec la 2e.
        FauxRepository enPanne = new FauxRepository() {
            @Override
            public Versement enregistrer(Versement versement) {
                if (versement.codeAgence().equals(AGENCE_A)) {
                    throw new IllegalStateException("panne simulée");
                }
                return super.enregistrer(versement);
            }
        };
        VersementsDemoListener resilient = new VersementsDemoListener(enPanne, HORLOGE);

        resilient.surAgencesDeclarees(new AgencesDeclareesEvent(List.of(AGENCE_A, AGENCE_B)));

        assertThat(enPanne.parAgence(AGENCE_A)).isEmpty();
        assertThat(enPanne.parAgence(AGENCE_B)).hasSize(2);
    }

    /** Repository en mémoire (mêmes contrats que le port). */
    private static class FauxRepository implements VersementRepository {
        private final Map<UUID, Versement> parId = new ConcurrentHashMap<>();

        @Override
        public Versement enregistrer(Versement versement) {
            parId.put(versement.id(), versement);
            return versement;
        }

        @Override
        public Optional<Versement> trouverParId(UUID id) {
            return Optional.ofNullable(parId.get(id));
        }

        @Override
        public Optional<Versement> trouverParReference(String reference) {
            return parId.values().stream().filter(v -> reference.equals(v.reference())).findFirst();
        }

        @Override
        public List<Versement> lister(FiltreVersement filtre) {
            return parId.values().stream()
                    .filter(v -> v.codeAgence().equals(filtre.codeAgence()))
                    .filter(v -> filtre.statut() == null || v.statut() == filtre.statut())
                    .toList();
        }

        @Override
        public long compterEnCours() {
            return parId.values().stream().filter(v -> v.statut().estEnCours()).count();
        }

        @Override
        public long compterReferences() {
            return parId.values().stream().filter(v -> v.reference() != null).count();
        }

        List<Versement> parAgence(String codeAgence) {
            return parId.values().stream().filter(v -> v.codeAgence().equals(codeAgence)).toList();
        }

        long total() {
            return parId.size();
        }
    }
}
