package dz.gam.poste.cotation.adapter.out.centralmock;

import dz.gam.poste.cotation.adapter.messaging.CotationStatutMessage;
import dz.gam.poste.cotation.config.CotationBus;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MOCK PROASSUR : simule l'impression de la quittance (client a accepté) → publie le
 * statut « Affaire gagnée » avec le n° de police. Exposé en HTTP pour piloter la démo /
 * les tests ; dans la réalité, l'événement viendrait de PROASSUR.
 */
@RestController
@RequestMapping("/api/mock/cotation")
public class ProassurMockQuittanceController {

    private static final Logger log = LoggerFactory.getLogger(ProassurMockQuittanceController.class);

    private final RabbitTemplate rabbitTemplate;

    public ProassurMockQuittanceController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/{reference}/quittance")
    public ResponseEntity<Void> imprimerQuittance(@PathVariable String reference,
                                                  @RequestParam(required = false) String numeroPolice) {
        String police = numeroPolice == null || numeroPolice.isBlank()
                ? "P-" + reference.replaceAll("[^0-9]", "")
                : numeroPolice;
        rabbitTemplate.convertAndSend(CotationBus.EXCHANGE, CotationBus.RK_COTATION_STATUT,
                new CotationStatutMessage(reference, StatutCotation.AFFAIRE_GAGNEE.name(),
                        null, null, null, police));
        log.info("[MOCK PROASSUR] Quittance imprimée pour {} — police {}", reference, police);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
