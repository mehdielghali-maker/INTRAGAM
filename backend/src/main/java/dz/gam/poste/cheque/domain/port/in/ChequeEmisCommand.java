package dz.gam.poste.cheque.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Commande d'entrée traduisant l'événement métier « un chèque a été émis par PROASSUR ».
 * Données brutes ; la validation métier se fait dans le domaine à la construction des
 * value objects.
 */
public record ChequeEmisCommand(
        String reference,
        BigDecimal montant,
        String devise,
        String beneficiaire,
        String agence,
        LocalDate dateEmission) {
}
