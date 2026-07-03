package dz.gam.poste.attestation.adapter.out.ocr;

import dz.gam.poste.attestation.domain.model.ResultatAttestation;
import dz.gam.poste.attestation.domain.port.out.LectureDocumentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Adapter RÉEL : appelle l'OCR MUTUALISÉ dans le conteneur reco (FastAPI) en multipart.
 *
 * Actif quand ocr.mode=http (voir application.yml). URL du service via ocr.base-url (jamais
 * en dur). Les champs JSON renvoyés par POST /lire-attestation mappent les composants du
 * record ResultatAttestation (contrat FIGÉ) ; tout champ supplémentaire est ignoré par Jackson.
 *
 * ACQUIS REPRIS TELS QUELS de RecoHttpAdapter (mêmes causes, mêmes remèdes) :
 * le corps multipart est construit À LA MAIN — la construction via MultiValueMap/
 * MultipartBodyBuilder de Spring n'envoyait pas correctement le filename de la pièce, et
 * FastAPI répondait alors 422 « photo manquante » (il ne voyait pas de fichier). Ici on
 * maîtrise boundary + en-têtes, comme curl.
 */
@Component
@ConditionalOnProperty(name = "ocr.mode", havingValue = "http")
public class LectureDocumentHttpAdapter implements LectureDocumentPort {

    private static final Logger log = LoggerFactory.getLogger(LectureDocumentHttpAdapter.class);

    private final RestClient client;

    public LectureDocumentHttpAdapter(@Value("${ocr.base-url}") String baseUrl) {
        // FORCER HTTP/1.1 : le client JDK tente HTTP/2 par défaut, or uvicorn/h11 (le
        // microservice) ne parle que HTTP/1.1 → « Invalid HTTP request received » et appel
        // perdu (cause déjà diagnostiquée sur RECO : curl en HTTP/1.1 passait, le backend
        // en HTTP/2 échouait).
        // TIMEOUTS obligatoires : sans eux, un service qui accepte la connexion mais ne
        // répond pas (inférence bloquée) gèlerait indéfiniment le thread de requête du poste.
        // À l'échéance, l'exception tombe dans le catch → repli neutre, la collecte continue.
        HttpClient jdk = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(jdk);
        fabrique.setReadTimeout(Duration.ofSeconds(30)); // OCR CPU : plusieurs secondes possibles
        this.client = RestClient.builder()
                .requestFactory(fabrique)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public ResultatAttestation lireAttestation(byte[] photo) {
        String boundary = "GamOcr" + UUID.randomUUID().toString().replace("-", "");
        byte[] corps = construireMultipart(photo, boundary);
        try {
            ResultatAttestation resultat = client.post()
                    .uri("/lire-attestation")
                    // .contentType() (et non .header) : sinon le convertisseur byte[] écrase le
                    // Content-Type en application/octet-stream et FastAPI ne parse pas le multipart.
                    .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                    .body(corps)
                    .retrieve()
                    .body(ResultatAttestation.class);
            return resultat != null ? resultat : ResultatAttestation.neutre();
        } catch (Exception e) {
            // L'OCR est un CONFORT : son indisponibilité ne doit JAMAIS bloquer la collecte
            // (saisie manuelle possible). On renvoie un résultat neutre — mais on TRACE.
            log.warn("OCR attestation indisponible (repli neutre) : {}", e.getMessage());
            return ResultatAttestation.neutre();
        }
    }

    /** Corps multipart/form-data : une seule pièce « photo » (fichier image). */
    private static byte[] construireMultipart(byte[] photo, String boundary) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"photo\"; filename=\"attestation.jpg\"\r\n"
                    + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(photo);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Construction du multipart OCR impossible", e);
        }
        return out.toByteArray();
    }
}
