package dz.gam.poste.proassurmock;

import dz.gam.poste.cheque.adapter.messaging.ChequeEmisMessage;
import dz.gam.poste.cheque.config.BusCheque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Adapter PROASSUR MOCK (émission). Simule l'ERP qui publie {@code ChequeEmis} sur le
 * bus. Exposé en HTTP pour piloter la démo et les tests ; dans la réalité, c'est
 * PROASSUR qui émettrait — pas une API du poste.
 */
@RestController
@RequestMapping("/api/mock/proassur/cheques")
public class ProassurMockPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProassurMockPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ProassurMockPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Corps de requête de simulation (devise optionnelle, défaut DZD). */
    public record EmettreChequeRequest(
            String reference,
            BigDecimal montant,
            String devise,
            String beneficiaire,
            String agence,
            java.time.LocalDate dateEmission) {
    }

    @PostMapping
    public ResponseEntity<Void> emettre(@RequestBody EmettreChequeRequest requete) {
        ChequeEmisMessage message = new ChequeEmisMessage(
                requete.reference(),
                requete.montant(),
                requete.devise() == null || requete.devise().isBlank() ? "DZD" : requete.devise(),
                requete.beneficiaire(),
                requete.agence(),
                requete.dateEmission());
        rabbitTemplate.convertAndSend(BusCheque.EXCHANGE, BusCheque.RK_CHEQUE_EMIS, message);
        log.info("[MOCK PROASSUR] ChequeEmis publié : référence={}", message.reference());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
