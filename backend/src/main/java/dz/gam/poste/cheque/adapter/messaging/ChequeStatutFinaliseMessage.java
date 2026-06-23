package dz.gam.poste.cheque.adapter.messaging;

import java.time.Instant;

/**
 * Contrat de message du bus pour l'événement <b>ChequeStatutFinalise</b>
 * (voir {@code contracts/events/cheque-statut-finalise.schema.json}).
 *
 * <p>Publié par le poste, consommé par l'adapter PROASSUR mock (write-back).
 * {@code statutFinal} est la valeur de {@code StatutCheque} (ENCAISSE | RETOURNE).
 */
public record ChequeStatutFinaliseMessage(
        String reference,
        String statutFinal,
        Instant dateFinalisation) {
}
