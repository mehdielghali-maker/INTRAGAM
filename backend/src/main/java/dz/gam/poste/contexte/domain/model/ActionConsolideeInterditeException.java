package dz.gam.poste.contexte.domain.model;

/**
 * Levée quand une action (création, dépôt…) est tentée en mode consolidé. Le consolidé est
 * une vue d'ensemble en LECTURE SEULE : une action doit toujours viser une agence précise.
 * Traduite en HTTP 409.
 */
public class ActionConsolideeInterditeException extends RuntimeException {

    public ActionConsolideeInterditeException() {
        super("Action impossible en mode consolidé : sélectionnez une agence précise.");
    }
}
