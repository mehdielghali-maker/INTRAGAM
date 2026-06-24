package dz.gam.poste.dpd.adapter.in.messaging;

import dz.gam.poste.dpd.adapter.messaging.StatutDpdMessage;
import dz.gam.poste.dpd.config.DpdBus;
import dz.gam.poste.dpd.domain.model.StatutDpd;
import dz.gam.poste.dpd.domain.port.in.RetourValidationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Adapter d'entrée (bus) : applique les retours de statut du central. */
@Component
public class StatutDpdListener {

    private static final Logger log = LoggerFactory.getLogger(StatutDpdListener.class);

    private final RetourValidationUseCase retour;

    public StatutDpdListener(RetourValidationUseCase retour) {
        this.retour = retour;
    }

    @RabbitListener(queues = DpdBus.QUEUE_STATUT)
    public void surStatut(StatutDpdMessage message) {
        StatutDpd statut = StatutDpd.valueOf(message.statut());
        log.info("Retour central DPD : référence={} statut={}", message.reference(), statut);
        switch (statut) {
            case EN_VALIDATION -> retour.mettreEnValidation(message.reference());
            case ACCORDEE -> retour.accorder(message.reference(), message.codeAccord());
            case REFUSEE -> retour.refuser(message.reference(), message.motif());
            default -> log.warn("Statut entrant non géré : {}", statut);
        }
    }
}
