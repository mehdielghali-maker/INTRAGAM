package dz.gam.poste.cheque.adapter.in.web;

import dz.gam.poste.cheque.domain.model.DossierIntrouvableException;
import dz.gam.poste.cheque.domain.model.TransitionStatutInvalideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier en réponses HTTP normalisées (RFC 7807 ProblemDetail).
 * Garde les contrôleurs et le domaine propres : la sémantique HTTP vit ici.
 */
@RestControllerAdvice
public class GestionErreursApi {

    @ExceptionHandler(DossierIntrouvableException.class)
    public ProblemDetail dossierIntrouvable(DossierIntrouvableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransitionStatutInvalideException.class)
    public ProblemDetail transitionInvalide(TransitionStatutInvalideException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail donneeInvalide(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
