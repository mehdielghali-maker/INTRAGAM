package dz.gam.poste.versement.adapter.out.messaging;

import dz.gam.poste.versement.adapter.messaging.VersementDeposeMessage;
import dz.gam.poste.versement.config.VersementBus;
import dz.gam.poste.versement.domain.event.VersementBancaireDeposeEvent;
import dz.gam.poste.versement.domain.port.out.PublicationVersementPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Adapter de sortie (bus) : publie « VersementBancaireDepose » vers le BPM. */
@Component
public class RabbitPublicationVersementAdapter implements PublicationVersementPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublicationVersementAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitPublicationVersementAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publier(VersementBancaireDeposeEvent evenement) {
        VersementDeposeMessage message = new VersementDeposeMessage(
                evenement.reference(), evenement.codeAgence(), evenement.moisSituation(),
                evenement.montantVerse(), evenement.dateVersement(), evenement.dateDepot());
        rabbitTemplate.convertAndSend(VersementBus.EXCHANGE, VersementBus.RK_VERSEMENT_DEPOSE, message);
        log.info("Publication VersementBancaireDepose : référence={} agence={}",
                message.reference(), message.codeAgence());
    }
}
