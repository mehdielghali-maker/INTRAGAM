package dz.gam.poste.reconnaissance.adapter.out.reco;

import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort;
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
import java.util.UUID;

/**
 * Adapter RÉEL : appelle le microservice RECO (FastAPI) en multipart.
 *
 * Actif quand reco.mode=http (voir application.yml). URL du service via reco.base-url (jamais en dur).
 * Les champs JSON renvoyés (estVehicule, typeVehicule, plaque, confiance, confianceVehicule) mappent
 * les composants du record ResultatReco ; le champ supplémentaire "vue" est ignoré par Jackson.
 *
 * Le corps multipart est construit À LA MAIN : la construction via MultiValueMap/MultipartBodyBuilder
 * de Spring n'envoyait pas correctement le filename de la pièce, et FastAPI répondait alors
 * 422 « photo manquante » (il ne voyait pas de fichier). Ici on maîtrise boundary + en-têtes, comme curl.
 */
@Component
@ConditionalOnProperty(name = "reco.mode", havingValue = "http")
public class RecoHttpAdapter implements ReconnaissancePort {

    private static final Logger log = LoggerFactory.getLogger(RecoHttpAdapter.class);

    private final RestClient client;

    public RecoHttpAdapter(@Value("${reco.base-url}") String baseUrl) {
        // FORCER HTTP/1.1 : le client JDK tente HTTP/2 par défaut, or uvicorn/h11 (le microservice)
        // ne parle que HTTP/1.1 → « Invalid HTTP request received » et appel perdu. C'est la cause
        // réelle (curl, en HTTP/1.1, fonctionnait ; le backend en HTTP/2 échouait).
        HttpClient jdk = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.client = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(jdk))
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public ResultatReco analyser(byte[] photo, String vue) {
        String boundary = "GamReco" + UUID.randomUUID().toString().replace("-", "");
        byte[] corps = construireMultipart(photo, vue, boundary);
        try {
            return client.post()
                    .uri("/analyser")
                    // .contentType() (et non .header) : sinon le convertisseur byte[] écrase le
                    // Content-Type en application/octet-stream et FastAPI ne parse pas le multipart.
                    .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                    .body(corps)
                    .retrieve()
                    .body(ResultatReco.class);
        } catch (Exception e) {
            // Le service RECO est un CONFORT : son indisponibilité ne doit JAMAIS bloquer la
            // déclaration. On renvoie un résultat neutre — mais on TRACE pour le diagnostic.
            log.warn("RECO indisponible (repli neutre) : {}", e.getMessage());
            return new ResultatReco(true, null, null, 0.0, 0.0);
        }
    }

    /** Corps multipart/form-data : pièce "photo" (fichier image) + champ texte "vue" optionnel. */
    private static byte[] construireMultipart(byte[] photo, String vue, String boundary) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"photo\"; filename=\"photo.jpg\"\r\n"
                    + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(photo);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            if (vue != null) {
                out.write(("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"vue\"\r\n\r\n"
                        + vue + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Construction du multipart RECO impossible", e);
        }
        return out.toByteArray();
    }
}
