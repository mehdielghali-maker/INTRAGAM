package dz.gam.poste.attestation.adapter.out.ocr;

import dz.gam.poste.attestation.domain.model.ResultatAttestation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LectureDocumentMockAdapterTest {

    private final LectureDocumentMockAdapter adapter = new LectureDocumentMockAdapter();

    @Test
    void lectures_variees_aux_formats_du_contrat() {
        List<ResultatAttestation> lectures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            lectures.add(adapter.lireAttestation(new byte[]{1}));
        }

        for (ResultatAttestation r : lectures) {
            assertThat(r.statut()).isIn(ResultatAttestation.STATUT_LU, ResultatAttestation.STATUT_A_VERIFIER);
            assertThat(r.confiance()).isBetween(0.0, 1.0);
            assertThat(r.texteBrut()).isNotBlank();
            if (r.numeroPolice() != null) {
                assertThat(r.numeroPolice()).matches("\\d{15}");       // police : 15 chiffres
            }
            if (r.numeroQuittance() != null) {
                assertThat(r.numeroQuittance()).matches("\\d{8}");     // quittance : 8 chiffres
            }
            if (r.codeAgence() != null) {
                assertThat(r.codeAgence()).matches("\\d{2}\\.[A-Z]{2}\\.\\d{4}"); // NN.AA.NNNN
            }
        }

        // Variété : plusieurs attestations différentes, dont au moins une douteuse
        // (parcours « à vérifier » du front) et une fiable.
        assertThat(lectures.stream().map(ResultatAttestation::numeroPolice).distinct().count())
                .isGreaterThan(1);
        assertThat(lectures).anyMatch(r -> ResultatAttestation.STATUT_A_VERIFIER.equals(r.statut()));
        assertThat(lectures).anyMatch(r -> ResultatAttestation.STATUT_LU.equals(r.statut()));
    }
}
