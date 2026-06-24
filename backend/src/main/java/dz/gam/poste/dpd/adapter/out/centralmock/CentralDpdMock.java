package dz.gam.poste.dpd.adapter.out.centralmock;

import dz.gam.poste.dpd.adapter.messaging.DemandePaiementDiffereEmiseMessage;
import dz.gam.poste.dpd.adapter.messaging.StatutDpdMessage;
import dz.gam.poste.dpd.config.DpdBus;
import dz.gam.poste.dpd.config.DpdProperties;
import dz.gam.poste.dpd.domain.model.StatutDpd;
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
 * MOCK du central. Sur réception d'une demande émise : prise en charge par un validateur
 * (BPM → EN_VALIDATION) puis validation de l'échéancier (PROASSUR → ACCORDEE + code accord).
 * Le refus se déclenche séparément (contrôleur mock). Délais configurables (0 en test).
 */
@Component
public class CentralDpdMock {

    private static final Logger log = LoggerFactory.getLogger(CentralDpdMock.class);

    private final RabbitTemplate rabbitTemplate;
    private final DpdProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "central-dpd-mock");
        t.setDaemon(true);
        return t;
    });

    public CentralDpdMock(RabbitTemplate rabbitTemplate, DpdProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @RabbitListener(queues = DpdBus.QUEUE_DEMANDE_EMISE)
    public void surDemandeEmise(DemandePaiementDiffereEmiseMessage message) {
        String reference = message.reference();
        String codeAccord = "AC-" + reference.replaceAll("[^0-9]", "");
        log.info("[MOCK CENTRAL DPD] Demande reçue {}", reference);

        long dValidation = properties.mock().delaiValidationMs();
        long dAccord = dValidation + properties.mock().delaiAccordMs();

        planifier(dValidation, () -> publier(new StatutDpdMessage(
                reference, StatutDpd.EN_VALIDATION.name(), null, null)));
        planifier(dAccord, () -> publier(new StatutDpdMessage(
                reference, StatutDpd.ACCORDEE.name(), codeAccord, null)));
    }

    private void planifier(long delaiMs, Runnable action) {
        scheduler.schedule(action, Math.max(0, delaiMs), TimeUnit.MILLISECONDS);
    }

    private void publier(StatutDpdMessage message) {
        rabbitTemplate.convertAndSend(DpdBus.EXCHANGE, DpdBus.RK_STATUT, message);
        log.info("[MOCK CENTRAL DPD] Statut renvoyé : référence={} statut={}", message.reference(), message.statut());
    }

    @PreDestroy
    void arret() {
        scheduler.shutdownNow();
    }
}
