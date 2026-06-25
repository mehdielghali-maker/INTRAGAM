package dz.gam.poste.versement.adapter.out.bpmmock;

import dz.gam.poste.versement.adapter.messaging.VersementDeposeMessage;
import dz.gam.poste.versement.adapter.messaging.VersementStatutMessage;
import dz.gam.poste.versement.config.VersementBus;
import dz.gam.poste.versement.config.VersementProperties;
import dz.gam.poste.versement.domain.model.StatutVersement;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MOCK du BPM / OneBase. Simule le traitement d'un versement déposé : prise « en contrôle »
 * (affectation d'une référence BPM) puis décision finale (validé par défaut, rejeté si
 * configuré). Remplaçable par la vraie intégration BPM sans toucher au domaine.
 * Délais et décision configurables ({@code poste.versement.mock.*}) — 0 en test.
 */
@Component
public class BpmOneBaseMock {

    private static final Logger log = LoggerFactory.getLogger(BpmOneBaseMock.class);

    private final RabbitTemplate rabbitTemplate;
    private final VersementProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "bpm-versement-mock");
        t.setDaemon(true);
        return t;
    });

    public BpmOneBaseMock(RabbitTemplate rabbitTemplate, VersementProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @RabbitListener(queues = VersementBus.QUEUE_VERSEMENT_DEPOSE)
    public void surVersementDepose(VersementDeposeMessage message) {
        String reference = message.reference();
        String suffixe = reference.replaceAll("[^0-9]", "");
        String referenceBpm = "BPM-" + suffixe;
        log.info("[MOCK BPM] Versement reçu {} — contrôle puis décision", reference);

        long delaiControle = properties.mock().delaiControleMs();
        long delaiDecision = delaiControle + properties.mock().delaiDecisionMs();
        StatutVersement decision = properties.mock().decision();

        // Prise en contrôle (BPM)
        planifier(delaiControle, () -> publier(new VersementStatutMessage(
                reference, StatutVersement.EN_CONTROLE.name(), referenceBpm, null)));

        // Décision finale (validé / rejeté)
        if (decision == StatutVersement.REJETE) {
            planifier(delaiDecision, () -> publier(new VersementStatutMessage(
                    reference, StatutVersement.REJETE.name(), referenceBpm,
                    "Pièce non conforme (contrôle BPM)")));
        } else {
            planifier(delaiDecision, () -> publier(new VersementStatutMessage(
                    reference, StatutVersement.VALIDE.name(), referenceBpm, null)));
        }
    }

    private void planifier(long delaiMs, Runnable action) {
        scheduler.schedule(action, Math.max(0, delaiMs), TimeUnit.MILLISECONDS);
    }

    private void publier(VersementStatutMessage message) {
        rabbitTemplate.convertAndSend(VersementBus.EXCHANGE, VersementBus.RK_VERSEMENT_STATUT, message);
        log.info("[MOCK BPM] Statut renvoyé : référence={} statut={}", message.reference(), message.statut());
    }

    @PreDestroy
    void arret() {
        scheduler.shutdownNow();
    }
}
