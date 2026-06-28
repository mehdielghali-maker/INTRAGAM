package dz.gam.poste.reconnaissance.adapter.out.reco;

import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Adapter RÉEL : appelle le microservice RECO (FastAPI) en multipart.
 *
 * Actif quand reco.mode=http (voir application.yml). URL du service via reco.base-url
 * (jamais en dur). Les noms de champs JSON renvoyés par le service (estVehicule,
 * typeVehicule, plaque, confiance, confianceVehicule) correspondent aux composants du
 * record ResultatReco : Jackson mappe seul.
 */
@Component
@ConditionalOnProperty(name = "reco.mode", havingValue = "http")
public class RecoHttpAdapter implements ReconnaissancePort {

    private final RestClient client;

    public RecoHttpAdapter(@Value("${reco.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ResultatReco analyser(byte[] photo, String vue) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new ByteArrayResource(photo) {
            @Override
            public String getFilename() {
                return "photo.jpg"; // requis pour un envoi multipart de fichier
            }
        });
        if (vue != null) {
            body.add("vue", vue);
        }

        try {
            return client.post()
                    .uri("/analyser")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(ResultatReco.class);
        } catch (Exception e) {
            // Le service RECO est un CONFORT : son indisponibilité ne doit JAMAIS
            // bloquer la déclaration. On renvoie un résultat neutre.
            return new ResultatReco(true, null, null, 0.0, 0.0);
        }
    }
}
