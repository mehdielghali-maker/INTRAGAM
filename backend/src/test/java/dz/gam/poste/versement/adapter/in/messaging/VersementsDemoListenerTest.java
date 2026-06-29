package dz.gam.poste.versement.adapter.in.messaging;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.versement.config.VersementProperties;
import dz.gam.poste.versement.domain.event.VersementBancaireDeposeEvent;
import dz.gam.poste.versement.domain.model.PieceJustificative;
import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.out.GedVersementPort;
import dz.gam.poste.versement.domain.port.out.ProductionEncaissePort;
import dz.gam.poste.versement.domain.port.out.PublicationVersementPort;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import dz.gam.poste.versement.domain.port.out.VersementsBanquePort;
import dz.gam.poste.versement.domain.service.VersementService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que le seed de démo des versements crée 2 entrées DÉPOSÉ par agence avec les valeurs
 * attendues, et qu'il est idempotent (un 2e événement ne duplique pas — garde
 * {@code existsByCodeAgence}).
 */
class VersementsDemoListenerTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-29T09:00:00Z"), ZoneOffset.UTC);
    private static final String AGENCE_A = "02.1.S.BENZERGA";
    private static final String AGENCE_B = "16.4.D.ALGER";

    private final FauxRepository repository = new FauxRepository();
    private final VersementsDemoListener listener = new VersementsDemoListener(service(), repository);

    private VersementService service() {
        ProductionEncaissePort production =
                (agence, mois) -> new ProductionEncaissePort.SituationProduction(BigDecimal.ZERO, BigDecimal.ZERO);
        VersementsBanquePort banque = (agence, mois) -> BigDecimal.ZERO;
        GedVersementPort ged = nom -> new PieceJustificative(nom, "GED-" + nom);
        VersementProperties props = new VersementProperties("VB", 3390,
                new VersementProperties.SeuilRegularisation(BigDecimal.valueOf(1_000_000), BigDecimal.TEN),
                List.of("BNA", "BEA"), List.of("2026-05"),
                new VersementProperties.Mock(0, 0, StatutVersement.VALIDE));
        return new VersementService(repository, new FauxPublication(), ged, production, banque, props, HORLOGE);
    }

    @Test
    void seme_deux_versements_deposes_par_agence_avec_les_valeurs_attendues() {
        listener.surAgencesDeclarees(new AgencesDeclareesEvent(List.of(AGENCE_A, AGENCE_B)));

        List<Versement> agenceA = repository.parAgence(AGENCE_A);
        assertThat(agenceA).hasSize(2);
        assertThat(agenceA).allSatisfy(v -> {
            assertThat(v.statut()).isEqualTo(StatutVersement.DEPOSE);
            assertThat(v.createur()).isEqualTo("Démo");
            assertThat(v.reference()).isNotNull();
            assertThat(v.pieces()).hasSize(1);
            assertThat(v.commentaire()).isEqualTo("Versement de démo");
        });
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

    /** Repository en mémoire (mêmes contrats que le port, dont {@code existsByCodeAgence}). */
    private static final class FauxRepository implements VersementRepository {
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
        public boolean existsByCodeAgence(String codeAgence) {
            return parId.values().stream().anyMatch(v -> v.codeAgence().equals(codeAgence));
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

    /** Port de publication no-op (les événements ne sont pas vérifiés ici). */
    private static final class FauxPublication implements PublicationVersementPort {
        private final List<VersementBancaireDeposeEvent> evenements = new ArrayList<>();

        @Override
        public void publier(VersementBancaireDeposeEvent evenement) {
            evenements.add(evenement);
        }
    }
}
