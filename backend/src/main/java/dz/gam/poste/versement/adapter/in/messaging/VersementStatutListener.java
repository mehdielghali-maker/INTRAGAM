package dz.gam.poste.versement.adapter.in.messaging;

import dz.gam.poste.versement.adapter.messaging.VersementStatutMessage;
import dz.gam.poste.versement.config.VersementBus;
import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.port.in.RetourBpmUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adapter d'entrée (bus) : applique les retours de statut du BPM. Traduit le message en
 * appel du use case ; aucune règle métier ici.
 */
@Component
public class VersementStatutListener {

    private static final Logger log = LoggerFactory.getLogger(VersementStatutListener.class);

    private final RetourBpmUseCase retourBpm;

    public VersementStatutListener(RetourBpmUseCase retourBpm) {
        this.retourBpm = retourBpm;
    }

    @RabbitListener(queues = VersementBus.QUEUE_VERSEMENT_STATUT)
    public void surVersementStatut(VersementStatutMessage message) {
        StatutVersement statut = StatutVersement.valueOf(message.statut());
        log.info("Retour BPM : référence={} statut={}", message.reference(), statut);
        switch (statut) {
            case EN_CONTROLE -> retourBpm.mettreEnControle(message.reference(), message.referenceBpm());
            case VALIDE -> retourBpm.valider(message.reference());
            case REJETE -> retourBpm.rejeter(message.reference(), message.motif());
            default -> log.warn("Statut entrant non géré : {}", statut);
        }
    }
}
