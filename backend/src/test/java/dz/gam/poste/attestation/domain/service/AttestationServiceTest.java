package dz.gam.poste.attestation.domain.service;

import dz.gam.poste.attestation.domain.model.LigneAttestation;
import dz.gam.poste.attestation.domain.model.ReleveDejaValideException;
import dz.gam.poste.attestation.domain.model.ReleveProduction;
import dz.gam.poste.attestation.domain.model.ResultatAttestation;
import dz.gam.poste.attestation.domain.model.StatutReleve;
import dz.gam.poste.attestation.domain.port.in.SoumettreReleveCommand;
import dz.gam.poste.attestation.domain.port.in.SoumettreReleveUseCase.RecuReleve;
import dz.gam.poste.attestation.domain.port.out.LectureDocumentPort;
import dz.gam.poste.attestation.domain.port.out.ProductionPort;
import dz.gam.poste.attestation.domain.port.out.ReleveProductionRepository;
import dz.gam.poste.contexte.domain.model.ActionConsolideeInterditeException;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttestationServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-07-03T10:00:00Z"), ZoneOffset.UTC);
    private static final Agence GHARBI = new Agence("16.I.GHARBI", "Agence Bab Ezzouar");
    private static final Agence HYDRA = new Agence("02.2.HYDRA", "Agence Hydra");

    private final FauxRepository repository = new FauxRepository();
    private final FauxProduction production = new FauxProduction();
    private final LectureDocumentPort lectureFixe = photo -> ResultatAttestation.neutre();

    private AttestationService service(FauxAgenceCourante agenceCourante) {
        return new AttestationService(lectureFixe, production, repository, agenceCourante, HORLOGE);
    }

    private static SoumettreReleveCommand commande(String mois, String... polices) {
        List<LigneAttestation> lignes = new ArrayList<>();
        for (String police : polices) {
            lignes.add(new LigneAttestation(police, "26004181", "00123-316-16", "BENALI Karim",
                    "01/01/2026", "31/12/2026", "24800,50", "16.AL.0316",
                    LigneAttestation.STATUT_CONFIRME));
        }
        return new SoumettreReleveCommand(mois, lignes);
    }

    @Test
    void soumission_ok_valide_le_lot_le_persiste_et_renvoie_l_accuse() {
        AttestationService service = service(FauxAgenceCourante.mono(GHARBI));

        RecuReleve recu = service.soumettre(commande("2026-06", "160312202600418", "160312202600505"));

        assertThat(recu.reference()).isEqualTo("PROD-2026-06-16.I.GHARBI");
        assertThat(recu.mois()).isEqualTo("2026-06");
        assertThat(recu.codeAgence()).isEqualTo(GHARBI.code()); // agence du CONTEXTE, pas du front
        assertThat(recu.nombreLignes()).isEqualTo(2);
        assertThat(recu.message()).contains("Production validée");

        ReleveProduction persiste = repository.trouverParAgenceEtMois(GHARBI.code(), "2026-06").orElseThrow();
        assertThat(persiste.statut()).isEqualTo(StatutReleve.VALIDEE);
        assertThat(persiste.dateValidation()).isEqualTo(HORLOGE.instant());
        assertThat(production.soumissions).hasSize(1);
    }

    @Test
    void deja_valide_pour_agence_et_mois_leve_l_exception_dediee_sans_resoumettre() {
        AttestationService service = service(FauxAgenceCourante.mono(GHARBI));
        service.soumettre(commande("2026-06", "160312202600418"));

        assertThatThrownBy(() -> service.soumettre(commande("2026-06", "160312202600505")))
                .isInstanceOf(ReleveDejaValideException.class)
                .hasMessageContaining(GHARBI.code())
                .hasMessageContaining("2026-06");
        // Le lot est verrouillé : rien n'est reparti vers la production.
        assertThat(production.soumissions).hasSize(1);
    }

    @Test
    void un_autre_mois_reste_soumissible_pour_la_meme_agence() {
        AttestationService service = service(FauxAgenceCourante.mono(GHARBI));
        service.soumettre(commande("2026-05", "160312202600418"));

        RecuReleve recu = service.soumettre(commande("2026-06", "160312202600418"));

        assertThat(recu.reference()).isEqualTo("PROD-2026-06-16.I.GHARBI");
        assertThat(production.soumissions).hasSize(2);
    }

    @Test
    void soumission_interdite_en_mode_consolide() {
        AttestationService service = service(FauxAgenceCourante.consolide(GHARBI, HYDRA));

        assertThatThrownBy(() -> service.soumettre(commande("2026-06", "160312202600418")))
                .isInstanceOf(ActionConsolideeInterditeException.class);
        assertThat(production.soumissions).isEmpty();
        assertThat(repository.parReference).isEmpty();
    }

    @Test
    void consultation_bornee_au_perimetre_actif() {
        // Un relevé dans le périmètre, un hors périmètre.
        repository.enregistrer(ReleveProduction.valider("2026-06", GHARBI.code(),
                commande("2026-06", "160312202600418").lignes(), HORLOGE.instant()));
        repository.enregistrer(ReleveProduction.valider("2026-06", "31.O.CENTRE",
                commande("2026-06", "310451202600772").lignes(), HORLOGE.instant()));

        List<ReleveProduction> releves = service(FauxAgenceCourante.mono(GHARBI)).relevesValides();

        assertThat(releves).hasSize(1);
        assertThat(releves.get(0).codeAgence()).isEqualTo(GHARBI.code());
    }

    @Test
    void lecture_delegue_au_port_ocr_et_exige_une_photo() {
        AttestationService service = service(FauxAgenceCourante.mono(GHARBI));

        assertThat(service.lire(new byte[]{1, 2, 3})).isEqualTo(ResultatAttestation.neutre());
        assertThatThrownBy(() -> service.lire(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("photo");
        assertThatThrownBy(() -> service.lire(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Faux contexte d'agence : mono-agence (action permise) ou consolidé (action interdite). */
    private record FauxAgenceCourante(boolean consolide, List<Agence> perimetre)
            implements AgenceCouranteQuery {

        static FauxAgenceCourante mono(Agence agence) {
            return new FauxAgenceCourante(false, List.of(agence));
        }

        static FauxAgenceCourante consolide(Agence... perimetre) {
            return new FauxAgenceCourante(true, List.of(perimetre));
        }

        @Override
        public boolean estConsolide() {
            return consolide;
        }

        @Override
        public Agence agencePourAction() {
            if (consolide) {
                throw new ActionConsolideeInterditeException();
            }
            return perimetre.get(0);
        }

        @Override
        public List<Agence> agencesActives() {
            return perimetre;
        }

        @Override
        public void exigerAcces(String codeAgence) {
            // sans objet pour ces tests
        }
    }

    /** Repository en mémoire. */
    private static final class FauxRepository implements ReleveProductionRepository {
        private final Map<String, ReleveProduction> parReference = new HashMap<>();

        @Override
        public ReleveProduction enregistrer(ReleveProduction releve) {
            parReference.put(releve.reference(), releve);
            return releve;
        }

        @Override
        public Optional<ReleveProduction> trouverParAgenceEtMois(String codeAgence, String mois) {
            return parReference.values().stream()
                    .filter(r -> r.codeAgence().equals(codeAgence) && r.mois().equals(mois))
                    .findFirst();
        }

        @Override
        public List<ReleveProduction> listerPourAgences(List<String> codesAgences) {
            return parReference.values().stream()
                    .filter(r -> codesAgences.contains(r.codeAgence()))
                    .toList();
        }
    }

    /** Port de production qui capture les soumissions (seam PROASSUR/Sage). */
    private static final class FauxProduction implements ProductionPort {
        private final List<ReleveProduction> soumissions = new ArrayList<>();

        @Override
        public RetourProduction soumettre(ReleveProduction releve) {
            soumissions.add(releve);
            return new RetourProduction(releve.reference(), "Production validée");
        }
    }
}
