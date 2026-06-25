package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.domain.model.ActionConsolideeInterditeException;
import dz.gam.poste.contexte.domain.model.AgenceHorsPerimetreException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les violations de périmètre d'agence en HTTP 403 (interdit). Le back ne fait
 * jamais confiance à l'agence transmise par le front : hors périmètre → rejet.
 */
@RestControllerAdvice
public class GestionErreursContexte {

    @ExceptionHandler(AgenceHorsPerimetreException.class)
    public ProblemDetail horsPerimetre(AgenceHorsPerimetreException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ActionConsolideeInterditeException.class)
    public ProblemDetail actionConsolidee(ActionConsolideeInterditeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
