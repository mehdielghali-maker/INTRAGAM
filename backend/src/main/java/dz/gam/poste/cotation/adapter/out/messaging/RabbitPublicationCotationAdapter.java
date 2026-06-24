package dz.gam.poste.cotation.adapter.out.messaging;

import dz.gam.poste.cotation.adapter.messaging.DemandeCotationEmiseMessage;
import dz.gam.poste.cotation.config.CotationBus;
import dz.gam.poste.cotation.domain.event.DemandeCotationEmiseEvent;
import dz.gam.poste.cotation.domain.port.out.PublicationCotationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Adapter de sortie (bus) : publie « DemandeCotationEmise » vers le central. */
@Component
public class RabbitPublicationCotationAdapter implements PublicationCotationPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublicationCotationAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitPublicationCotationAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publier(DemandeCotationEmiseEvent evenement) {
        DemandeCotationEmiseMessage message = new DemandeCotationEmiseMessage(
                evenement.reference().valeur(), evenement.objet(), evenement.nomProspect(),
                evenement.numeroPolice(), evenement.commentaire(), evenement.codeAgence(),
                evenement.directionRegionale());
        rabbitTemplate.convertAndSend(CotationBus.EXCHANGE, CotationBus.RK_DEMANDE_EMISE, message);
        log.info("Publication DemandeCotationEmise : référence={}", message.reference());
    }
}
