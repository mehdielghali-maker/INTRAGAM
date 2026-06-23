package dz.gam.poste.cheque.domain.port.in;

import dz.gam.poste.cheque.domain.model.DossierCheque;

/**
 * Port d'entrée : ouvrir un dossier de suivi à partir d'un chèque émis par PROASSUR.
 * Doit être idempotent vis-à-vis de la référence (le bus peut redélivrer l'événement).
 */
public interface EnregistrerChequeEmisUseCase {

    DossierCheque enregistrer(ChequeEmisCommand commande);
}
