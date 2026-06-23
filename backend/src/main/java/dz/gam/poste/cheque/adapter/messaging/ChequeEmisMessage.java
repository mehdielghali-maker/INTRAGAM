package dz.gam.poste.cheque.adapter.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrat de message du bus pour l'événement <b>ChequeEmis</b>
 * (voir {@code contracts/events/cheque-emis.schema.json}).
 *
 * <p>DTO de transport : il vit à la frontière (adapter), pas dans le domaine. Il est
 * partagé entre le consommateur du poste et l'adapter PROASSUR mock qui le produit.
 */
public record ChequeEmisMessage(
        String reference,
        BigDecimal montant,
        String devise,
        String beneficiaire,
        String agence,
        LocalDate dateEmission) {
}
