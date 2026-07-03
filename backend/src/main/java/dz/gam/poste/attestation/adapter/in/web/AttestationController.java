package dz.gam.poste.attestation.adapter.in.web;

import dz.gam.poste.attestation.adapter.in.web.dto.LectureAttestationResponse;
import dz.gam.poste.attestation.adapter.in.web.dto.ReleveRecuResponse;
import dz.gam.poste.attestation.adapter.in.web.dto.ReleveValideResponse;
import dz.gam.poste.attestation.adapter.in.web.dto.SoumettreReleveRequest;
import dz.gam.poste.attestation.domain.port.in.ConsulterRelevesUseCase;
import dz.gam.poste.attestation.domain.port.in.LireAttestationUseCase;
import dz.gam.poste.attestation.domain.port.in.SoumettreReleveUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

/**
 * Adapter d'entrée (web) du module « Attestations ». Ne fait que traduire HTTP ↔ use cases ;
 * aucune règle métier ici. Endpoints protégés par l'authentification (session requise) et
 * par l'accès au module « attestations » (AccesModuleInterceptor). L'AGENCE vient toujours
 * du contexte serveur — jamais d'un paramètre transmis par le front.
 */
@RestController
@RequestMapping("/api/attestations")
public class AttestationController {

    private final LireAttestationUseCase lire;
    private final SoumettreReleveUseCase soumettre;
    private final ConsulterRelevesUseCase consulter;

    public AttestationController(LireAttestationUseCase lire, SoumettreReleveUseCase soumettre,
                                 ConsulterRelevesUseCase consulter) {
        this.lire = lire;
        this.soumettre = soumettre;
        this.consulter = consulter;
    }

    /** Lit une attestation photographiée (OCR) pour pré-remplir une ligne du relevé. */
    @PostMapping("/lire")
    public LectureAttestationResponse lire(@RequestParam("photo") MultipartFile photo) {
        byte[] contenu;
        try {
            contenu = photo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo illisible");
        }
        return LectureAttestationResponse.de(lire.lire(contenu));
    }

    /** Valide et soumet le relevé du mois pour l'agence ACTIVE (409 si déjà validé/consolidé). */
    @PostMapping("/releves")
    @ResponseStatus(HttpStatus.CREATED)
    public ReleveRecuResponse soumettre(@RequestBody SoumettreReleveRequest requete) {
        return ReleveRecuResponse.de(soumettre.soumettre(requete.versCommande()));
    }

    /** Relevés VALIDÉS du périmètre actif (agence active, ou tout le périmètre en consolidé). */
    @GetMapping("/releves")
    public List<ReleveValideResponse> releves() {
        return consulter.relevesValides().stream().map(ReleveValideResponse::de).toList();
    }
}
