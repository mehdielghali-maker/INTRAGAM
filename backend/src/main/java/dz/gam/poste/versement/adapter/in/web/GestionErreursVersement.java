package dz.gam.poste.versement.adapter.in.web;

import dz.gam.poste.versement.domain.model.TransitionVersementInvalideException;
import dz.gam.poste.versement.domain.model.VersementIntrouvableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier du versement en réponses HTTP. (Les
 * {@code IllegalArgumentException} de validation — reçu/montant — sont gérées globalement en 400.)
 */
@RestControllerAdvice
public class GestionErreursVersement {

    @ExceptionHandler(VersementIntrouvableException.class)
    public ProblemDetail introuvable(VersementIntrouvableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransitionVersementInvalideException.class)
    public ProblemDetail transitionInvalide(TransitionVersementInvalideException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
