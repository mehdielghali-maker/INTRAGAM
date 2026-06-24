package dz.gam.poste.dpd.adapter.out.centralmock;

import dz.gam.poste.dpd.adapter.messaging.StatutDpdMessage;
import dz.gam.poste.dpd.config.DpdBus;
import dz.gam.poste.dpd.domain.model.StatutDpd;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MOCK central : déclenche un REFUS (avec motif) sur une demande. Exposé en HTTP pour
 * piloter la démo / les tests ; en réalité l'événement viendrait du central.
 */
@RestController
@RequestMapping("/api/mock/dpd")
public class CentralDpdMockController {

    private final RabbitTemplate rabbitTemplate;

    public CentralDpdMockController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/{reference}/refuser")
    public ResponseEntity<Void> refuser(@PathVariable String reference,
                                        @RequestParam(required = false) String motif) {
        String m = motif == null || motif.isBlank() ? "Antériorité d'impayés" : motif;
        rabbitTemplate.convertAndSend(DpdBus.EXCHANGE, DpdBus.RK_STATUT,
                new StatutDpdMessage(reference, StatutDpd.REFUSEE.name(), null, m));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
