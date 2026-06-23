package dz.gam.poste.cheque.domain.port.out;

import dz.gam.poste.cheque.domain.event.ChequeStatutFinaliseEvent;

/**
 * Port de sortie : publication des événements de domaine vers le monde extérieur
 * (le bus). C'est par ici que se referme la boucle vers PROASSUR (ADR 0003).
 *
 * <p>Le domaine dépend de cette interface, jamais de RabbitMQ directement.
 */
public interface PublicationEvenementPort {

    void publier(ChequeStatutFinaliseEvent evenement);
}
