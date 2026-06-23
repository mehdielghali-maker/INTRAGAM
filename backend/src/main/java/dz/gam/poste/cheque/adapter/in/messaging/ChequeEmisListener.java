package dz.gam.poste.cheque.adapter.in.messaging;

import dz.gam.poste.cheque.adapter.messaging.ChequeEmisMessage;
import dz.gam.poste.cheque.config.BusCheque;
import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.port.in.ChequeEmisCommand;
import dz.gam.poste.cheque.domain.port.in.EnregistrerChequeEmisUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adapter d'entrée (bus) : consomme l'événement {@code ChequeEmis} émis par PROASSUR
 * et délègue au use case. Il ne contient aucune règle métier — uniquement la traduction
 * message → commande.
 */
@Component
public class ChequeEmisListener {

    private static final Logger log = LoggerFactory.getLogger(ChequeEmisListener.class);

    private final EnregistrerChequeEmisUseCase enregistrerChequeEmis;

    public ChequeEmisListener(EnregistrerChequeEmisUseCase enregistrerChequeEmis) {
        this.enregistrerChequeEmis = enregistrerChequeEmis;
    }

    @RabbitListener(queues = BusCheque.QUEUE_CHEQUE_EMIS)
    public void surChequeEmis(ChequeEmisMessage message) {
        log.info("Réception ChequeEmis depuis PROASSUR : référence={}", message.reference());
        ChequeEmisCommand commande = new ChequeEmisCommand(
                message.reference(),
                message.montant(),
                message.devise(),
                message.beneficiaire(),
                message.agence(),
                message.dateEmission());
        DossierCheque dossier = enregistrerChequeEmis.enregistrer(commande);
        log.info("Dossier de suivi ouvert : id={} référence={} statut={}",
                dossier.id(), dossier.reference(), dossier.statut());
    }
}
