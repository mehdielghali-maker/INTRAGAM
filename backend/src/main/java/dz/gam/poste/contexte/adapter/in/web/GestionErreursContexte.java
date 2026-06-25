package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.domain.model.ActionConsolideeInterditeException;
import dz.gam.poste.contexte.domain.model.AgenceHorsPerimetreException;
import dz.gam.poste.contexte.domain.model.IdentifiantsInvalidesException;
import dz.gam.poste.contexte.domain.model.MotDePasseActuelInvalideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les erreurs métier transverses en codes HTTP : périmètre d'agence (403), action en
 * consolidé (409), identifiants de connexion invalides (401), mot de passe actuel erroné (400).
 * Le back ne fait jamais confiance aux données transmises par le front.
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

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ProblemDetail identifiantsInvalides(IdentifiantsInvalidesException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(MotDePasseActuelInvalideException.class)
    public ProblemDetail motDePasseActuelInvalide(MotDePasseActuelInvalideException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
