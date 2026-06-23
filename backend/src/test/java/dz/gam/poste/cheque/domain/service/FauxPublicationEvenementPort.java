package dz.gam.poste.cheque.domain.service;

import dz.gam.poste.cheque.domain.event.ChequeStatutFinaliseEvent;
import dz.gam.poste.cheque.domain.port.out.PublicationEvenementPort;

import java.util.ArrayList;
import java.util.List;

/** Double de test en mémoire du port de publication (aucun bus réel). */
class FauxPublicationEvenementPort implements PublicationEvenementPort {

    final List<ChequeStatutFinaliseEvent> publies = new ArrayList<>();

    @Override
    public void publier(ChequeStatutFinaliseEvent evenement) {
        publies.add(evenement);
    }
}
