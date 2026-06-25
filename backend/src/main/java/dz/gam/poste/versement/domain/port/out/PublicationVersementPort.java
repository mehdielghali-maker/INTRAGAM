package dz.gam.poste.versement.domain.port.out;

import dz.gam.poste.versement.domain.event.VersementBancaireDeposeEvent;

/** Port de sortie : publication de l'événement « VersementBancaireDepose » sur le bus. */
public interface PublicationVersementPort {

    void publier(VersementBancaireDeposeEvent evenement);
}
