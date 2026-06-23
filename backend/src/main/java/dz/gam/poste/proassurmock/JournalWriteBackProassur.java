package dz.gam.poste.proassurmock;

import dz.gam.poste.cheque.adapter.messaging.ChequeStatutFinaliseMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Journal en mémoire des write-backs reçus par le mock PROASSUR. Sert l'observabilité
 * locale et permet au test d'intégration d'affirmer que la boucle s'est refermée.
 *
 * <p>Quand la vraie API PROASSUR existera, ce journal disparaît : l'adapter réel
 * appellera l'ERP au lieu de mémoriser.
 */
@Component
public class JournalWriteBackProassur {

    private final List<ChequeStatutFinaliseMessage> writeBacks = new CopyOnWriteArrayList<>();

    public void enregistrer(ChequeStatutFinaliseMessage message) {
        writeBacks.add(message);
    }

    public List<ChequeStatutFinaliseMessage> writeBacks() {
        return List.copyOf(writeBacks);
    }
}
