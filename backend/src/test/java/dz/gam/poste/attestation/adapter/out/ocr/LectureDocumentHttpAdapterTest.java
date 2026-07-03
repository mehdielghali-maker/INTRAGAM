package dz.gam.poste.attestation.adapter.out.ocr;

import dz.gam.poste.attestation.domain.model.ResultatAttestation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste le DURCISSEMENT de l'adapter HTTP sans aucun réseau : l'URL pointe un port local
 * fermé → connexion refusée immédiatement → le catch doit rendre le repli NEUTRE (l'OCR
 * ne bloque jamais la collecte). Le chemin nominal (multipart, HTTP/1.1) reprend les acquis
 * éprouvés de RecoHttpAdapter et se vérifie avec le service réel (hors périmètre unitaire).
 */
class LectureDocumentHttpAdapterTest {

    @Test
    void service_injoignable_rend_le_repli_neutre_sans_bloquer() {
        // Port 1 en local : aucun service n'y écoute, la connexion est refusée tout de suite.
        LectureDocumentHttpAdapter adapter = new LectureDocumentHttpAdapter("http://127.0.0.1:1");

        ResultatAttestation resultat = adapter.lireAttestation(new byte[]{1, 2, 3});

        assertThat(resultat).isEqualTo(ResultatAttestation.neutre());
        assertThat(resultat.numeroPolice()).isNull();
        assertThat(resultat.confiance()).isZero();
        assertThat(resultat.statut()).isEqualTo(ResultatAttestation.STATUT_A_VERIFIER);
    }
}
