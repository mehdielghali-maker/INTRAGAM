package dz.gam.poste.cheque.adapter.out.messaging;

import dz.gam.poste.cheque.adapter.messaging.ChequeStatutFinaliseMessage;
import dz.gam.poste.cheque.config.BusCheque;
import dz.gam.poste.cheque.domain.event.ChequeStatutFinaliseEvent;
import dz.gam.poste.cheque.domain.port.out.PublicationEvenementPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter de sortie (bus) : implémente le port de publication en envoyant
 * {@code ChequeStatutFinalise} sur l'exchange. C'est l'extrémité « émettrice » de la
 * boucle fermée vers PROASSUR.
 */
@Component
public class RabbitPublicationAdapter implements PublicationEvenementPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublicationAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitPublicationAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publier(ChequeStatutFinaliseEvent evenement) {
        ChequeStatutFinaliseMessage message = new ChequeStatutFinaliseMessage(
                evenement.reference().valeur(),
                evenement.statutFinal().name(),
                evenement.dateFinalisation());
        rabbitTemplate.convertAndSend(BusCheque.EXCHANGE, BusCheque.RK_CHEQUE_STATUT_FINALISE, message);
        log.info("Publication ChequeStatutFinalise : référence={} statut={}",
                message.reference(), message.statutFinal());
    }
}
