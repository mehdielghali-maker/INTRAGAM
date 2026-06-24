package dz.gam.poste.dpd.adapter.out.centralmock;

import dz.gam.poste.dpd.adapter.messaging.EcheancierDpdMisAJourMessage;
import dz.gam.poste.dpd.config.DpdBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * MOCK « Suivi des échéanciers » : consomme {@code EcheancierDpdMisAJour} pour matérialiser
 * le handoff (la fonction réelle n'est pas encore implémentée). Ici, on journalise.
 */
@Component
public class SuiviEcheanciersMockListener {

    private static final Logger log = LoggerFactory.getLogger(SuiviEcheanciersMockListener.class);

    @RabbitListener(queues = DpdBus.QUEUE_MAJ)
    public void surMaj(EcheancierDpdMisAJourMessage message) {
        log.info("[MOCK SUIVI ÉCHÉANCIERS] Échéancier synchronisé : accord={} version={}",
                message.codeAccord(), message.version());
    }
}
