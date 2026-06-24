package dz.gam.poste.dpd.adapter.out.messaging;

import dz.gam.poste.dpd.adapter.messaging.DemandePaiementDiffereEmiseMessage;
import dz.gam.poste.dpd.adapter.messaging.EcheancierDpdMisAJourMessage;
import dz.gam.poste.dpd.config.DpdBus;
import dz.gam.poste.dpd.domain.event.DemandePaiementDiffereEmiseEvent;
import dz.gam.poste.dpd.domain.event.EcheancierDpdMisAJourEvent;
import dz.gam.poste.dpd.domain.port.out.PublicationDpdPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Adapter de sortie (bus) : publie les événements DPD. */
@Component
public class RabbitPublicationDpdAdapter implements PublicationDpdPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublicationDpdAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitPublicationDpdAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publier(DemandePaiementDiffereEmiseEvent e) {
        rabbitTemplate.convertAndSend(DpdBus.EXCHANGE, DpdBus.RK_DEMANDE_EMISE,
                new DemandePaiementDiffereEmiseMessage(e.reference().valeur(), e.noProposition(),
                        e.nomAssure(), e.montantPrime(), e.dureeContratMois()));
        log.info("Publication DemandePaiementDiffereEmise : référence={}", e.reference());
    }

    @Override
    public void publier(EcheancierDpdMisAJourEvent e) {
        rabbitTemplate.convertAndSend(DpdBus.EXCHANGE, DpdBus.RK_MAJ,
                new EcheancierDpdMisAJourMessage(e.codeAccord(), e.version()));
        log.info("Publication EcheancierDpdMisAJour : accord={} version={}", e.codeAccord(), e.version());
    }
}
