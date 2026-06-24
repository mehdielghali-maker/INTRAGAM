package dz.gam.poste.cotation.adapter.in.messaging;

import dz.gam.poste.cotation.adapter.messaging.CotationStatutMessage;
import dz.gam.poste.cotation.config.CotationBus;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import dz.gam.poste.cotation.domain.port.in.RetourCentralUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adapter d'entrée (bus) : applique les retours de statut du central. Traduit le message
 * en appel du use case ; aucune règle métier ici.
 */
@Component
public class CotationStatutListener {

    private static final Logger log = LoggerFactory.getLogger(CotationStatutListener.class);

    private final RetourCentralUseCase retourCentral;

    public CotationStatutListener(RetourCentralUseCase retourCentral) {
        this.retourCentral = retourCentral;
    }

    @RabbitListener(queues = CotationBus.QUEUE_COTATION_STATUT)
    public void surCotationStatut(CotationStatutMessage message) {
        StatutCotation statut = StatutCotation.valueOf(message.statut());
        log.info("Retour central : référence={} statut={}", message.reference(), statut);
        switch (statut) {
            case EN_COURS -> retourCentral.prendreEnCharge(message.reference(), message.souscripteur());
            case A_FINALISER -> retourCentral.finaliser(message.reference(),
                    message.numeroProposition(), message.referenceDevis());
            case AFFAIRE_GAGNEE -> retourCentral.marquerAffaireGagnee(message.reference(), message.numeroPolice());
            default -> log.warn("Statut entrant non géré : {}", statut);
        }
    }
}
