package dz.gam.poste.cotation.adapter.in.web;

import dz.gam.poste.cotation.domain.model.DemandeIntrouvableException;
import dz.gam.poste.cotation.domain.model.TransitionCotationInvalideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier de la cotation en réponses HTTP. (Les
 * {@code IllegalArgumentException} de validation sont déjà gérées globalement en 400.)
 */
@RestControllerAdvice
public class GestionErreursCotation {

    @ExceptionHandler(DemandeIntrouvableException.class)
    public ProblemDetail introuvable(DemandeIntrouvableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransitionCotationInvalideException.class)
    public ProblemDetail transitionInvalide(TransitionCotationInvalideException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
