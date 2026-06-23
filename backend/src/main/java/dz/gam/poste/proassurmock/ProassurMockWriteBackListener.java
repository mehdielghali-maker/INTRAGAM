package dz.gam.poste.proassurmock;

import dz.gam.poste.cheque.adapter.messaging.ChequeStatutFinaliseMessage;
import dz.gam.poste.cheque.config.BusCheque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adapter PROASSUR MOCK (write-back). Simule la mise à jour du règlement dans l'ERP à la
 * réception de {@code ChequeStatutFinalise} : ici, on journalise. À remplacer par un
 * appel à la vraie API PROASSUR sans toucher au code métier (ADR 0002).
 */
@Component
public class ProassurMockWriteBackListener {

    private static final Logger log = LoggerFactory.getLogger(ProassurMockWriteBackListener.class);

    private final JournalWriteBackProassur journal;

    public ProassurMockWriteBackListener(JournalWriteBackProassur journal) {
        this.journal = journal;
    }

    @RabbitListener(queues = BusCheque.QUEUE_WRITE_BACK)
    public void surChequeStatutFinalise(ChequeStatutFinaliseMessage message) {
        log.info("WRITE-BACK PROASSUR (simulé) : règlement de la référence {} mis à jour — statut {} @ {}",
                message.reference(), message.statutFinal(), message.dateFinalisation());
        journal.enregistrer(message);
    }
}
