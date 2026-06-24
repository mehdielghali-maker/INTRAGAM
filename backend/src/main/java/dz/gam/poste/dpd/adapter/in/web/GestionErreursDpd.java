package dz.gam.poste.dpd.adapter.in.web;

import dz.gam.poste.dpd.domain.model.DpdExceptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduit les exceptions métier DPD en réponses HTTP. */
@RestControllerAdvice
public class GestionErreursDpd {

    @ExceptionHandler({DpdExceptions.DemandeIntrouvable.class, DpdExceptions.AccordIntrouvable.class})
    public ProblemDetail introuvable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({DpdExceptions.TransitionInvalide.class, DpdExceptions.RcManquant.class})
    public ProblemDetail conflit(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
