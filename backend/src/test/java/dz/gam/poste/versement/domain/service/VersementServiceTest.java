package dz.gam.poste.versement.domain.service;

import dz.gam.poste.versement.config.VersementProperties;
import dz.gam.poste.versement.domain.event.VersementBancaireDeposeEvent;
import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.model.PieceJustificative;
import dz.gam.poste.versement.domain.model.SituationMois;
import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.in.DeposerVersementCommand;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.out.GedVersementPort;
import dz.gam.poste.versement.domain.port.out.ProductionEncaissePort;
import dz.gam.poste.versement.domain.port.out.PublicationVersementPort;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import dz.gam.poste.versement.domain.port.out.VersementsBanquePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersementServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-06-24T09:00:00Z"), ZoneOffset.UTC);
    private static final String AGENCE = "02.1.S.BENZERGA";
    private static final String CREATEUR = "M. Benzerga";

    private final FauxRepository repository = new FauxRepository();
    private final FauxPublication publication = new FauxPublication();

    private VersementService service(BigDecimal production, BigDecimal encaisse, BigDecimal verse) {
        ProductionEncaissePort prod = (agence, mois) -> new ProductionEncaissePort.SituationProduction(production, encaisse);
        VersementsBanquePort banque = (agence, mois) -> verse;
        GedVersementPort ged = nom -> new PieceJustificative(nom, "GED-TEST");
        VersementProperties props = new VersementProperties("VB", 3390,
                new VersementProperties.SeuilRegularisation(BigDecimal.valueOf(1_000_000), BigDecimal.TEN),
                List.of("AGB"), List.of("2026-06"),
                new VersementProperties.Mock(0, 0, StatutVersement.VALIDE));
        return new VersementService(repository, publication, ged, prod, banque, props, HORLOGE);
    }

    private DeposerVersementCommand commande(BigDecimal montant, List<String> fichiers) {
        return new DeposerVersementCommand("2026-06", montant, LocalDate.parse("2026-06-24"),
                "BRD-3391", "AGB", "RAS", fichiers);
    }

    @Test
    void situation_du_mois_reste_a_regulariser_egale_encaisse_moins_verse() {
        SituationMois s = service(bd(9_200_000), bd(7_850_000), bd(6_400_000))
                .situation(AGENCE, MoisSituation.depuis("2026-06"));

        assertThat(s.productionEmise()).isEqualByComparingTo("9200000");
        assertThat(s.encaisse()).isEqualByComparingTo("7850000");
        assertThat(s.dejaVerse()).isEqualByComparingTo("6400000");
        // Reste = encaissé − versé (même définition que l'accueil)
        assertThat(s.resteARegulariser()).isEqualByComparingTo("1450000");
        assertThat(s.aRegulariser()).isTrue(); // 1 450 000 ≥ seuil 1 000 000
    }

    @Test
    void soumettre_sans_recu_est_refuse() {
        VersementService service = service(bd(9_200_000), bd(7_850_000), bd(6_400_000));

        assertThatThrownBy(() -> service.soumettre(AGENCE, CREATEUR, commande(bd(1_450_000), List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reçu de versement est obligatoire");
        assertThat(publication.evenements).isEmpty();
    }

    @Test
    void soumettre_avec_montant_non_positif_est_refuse() {
        VersementService service = service(bd(9_200_000), bd(7_850_000), bd(6_400_000));

        assertThatThrownBy(() -> service.soumettre(AGENCE, CREATEUR,
                commande(BigDecimal.ZERO, List.of("recu.pdf"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montant doit être strictement positif");
    }

    @Test
    void soumettre_depose_publie_l_evenement_et_genere_la_reference() {
        VersementService service = service(bd(9_200_000), bd(7_850_000), bd(6_400_000));

        Versement v = service.soumettre(AGENCE, CREATEUR, commande(bd(1_450_000), List.of("recu.pdf")));

        assertThat(v.statut()).isEqualTo(StatutVersement.DEPOSE);
        assertThat(v.reference()).isEqualTo("VB-2026-3391"); // seed 3390 + 0 + 1
        assertThat(v.pieces()).hasSize(1);
        assertThat(publication.evenements).hasSize(1);
        VersementBancaireDeposeEvent ev = publication.evenements.get(0);
        assertThat(ev.reference()).isEqualTo("VB-2026-3391");
        assertThat(ev.codeAgence()).isEqualTo(AGENCE);
        assertThat(ev.montantVerse()).isEqualByComparingTo("1450000");
    }

    @Test
    void boucle_bpm_depose_puis_en_controle_puis_valide() {
        VersementService service = service(bd(9_200_000), bd(7_850_000), bd(6_400_000));
        Versement v = service.soumettre(AGENCE, CREATEUR, commande(bd(1_450_000), List.of("recu.pdf")));
        String ref = v.reference();

        assertThat(service.compterEnCours()).isEqualTo(1);

        service.mettreEnControle(ref, "BPM-3391");
        assertThat(repository.trouverParReference(ref).orElseThrow().statut()).isEqualTo(StatutVersement.EN_CONTROLE);

        service.valider(ref);
        assertThat(repository.trouverParReference(ref).orElseThrow().statut()).isEqualTo(StatutVersement.VALIDE);
        // Plus en cours une fois validé (terminal).
        assertThat(service.compterEnCours()).isZero();
    }

    @Test
    void rejet_porte_le_motif() {
        VersementService service = service(bd(9_200_000), bd(7_850_000), bd(6_400_000));
        Versement v = service.soumettre(AGENCE, CREATEUR, commande(bd(1_450_000), List.of("recu.pdf")));
        service.mettreEnControle(v.reference(), "BPM-3391");

        service.rejeter(v.reference(), "Pièce illisible");

        Versement rejete = repository.trouverParReference(v.reference()).orElseThrow();
        assertThat(rejete.statut()).isEqualTo(StatutVersement.REJETE);
        assertThat(rejete.motifRejet()).isEqualTo("Pièce illisible");
    }

    private static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }

    /** Repository en mémoire. */
    private static final class FauxRepository implements VersementRepository {
        private final Map<UUID, Versement> parId = new HashMap<>();

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
    }

    /** Port de publication qui capture les événements. */
    private static final class FauxPublication implements PublicationVersementPort {
        private final List<VersementBancaireDeposeEvent> evenements = new ArrayList<>();

        @Override
        public void publier(VersementBancaireDeposeEvent evenement) {
            evenements.add(evenement);
        }
    }
}
