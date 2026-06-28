package dz.gam.poste.reconnaissance.adapter.in.web;

import dz.gam.poste.reconnaissance.adapter.in.web.dto.VerificationVehiculeResponse;
import dz.gam.poste.reconnaissance.domain.model.ResultatVerificationVehicule;
import dz.gam.poste.reconnaissance.domain.port.in.VerifierVehiculeUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Adapter d'entrée (web) : expose la vérification de véhicule en REST. Ne fait que
 * traduire HTTP ↔ use case ; aucune règle métier ici (la comparaison plaque ↔ contrat
 * est faite côté domaine). Endpoint protégé par l'authentification (session requise)
 * comme toute API hors /api/auth/**.
 */
@RestController
@RequestMapping("/api/reconnaissance")
public class ReconnaissanceController {

    private final VerifierVehiculeUseCase verifier;

    public ReconnaissanceController(VerifierVehiculeUseCase verifier) {
        this.verifier = verifier;
    }

    /**
     * Analyse une photo de véhicule et la compare à l'immatriculation du contrat.
     *
     * @param photo           image (multipart)
     * @param vue             avant / arriere / gauche / droite / toit (facultatif)
     * @param immatriculation immatriculation du contrat (pré-remplie PROASSUR)
     */
    @PostMapping("/analyser")
    public VerificationVehiculeResponse analyser(@RequestParam("photo") MultipartFile photo,
                                                 @RequestParam(value = "vue", required = false) String vue,
                                                 @RequestParam("immatriculation") String immatriculation) {
        byte[] contenu;
        try {
            contenu = photo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo illisible");
        }
        ResultatVerificationVehicule resultat = verifier.verifier(contenu, vue, immatriculation);
        return VerificationVehiculeResponse.de(resultat);
    }
}
