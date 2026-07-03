package dz.gam.poste.attestation.adapter.in.web;

import dz.gam.poste.attestation.domain.model.ReleveDejaValideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier des attestations en réponses HTTP. Les
 * {@code IllegalArgumentException} de validation (payload invalide) sont déjà gérées
 * globalement en 400, et l'action en consolidé en 409 (GestionErreursContexte).
 */
@RestControllerAdvice
public class GestionErreursAttestation {

    @ExceptionHandler(ReleveDejaValideException.class)
    public ProblemDetail dejaValide(ReleveDejaValideException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
