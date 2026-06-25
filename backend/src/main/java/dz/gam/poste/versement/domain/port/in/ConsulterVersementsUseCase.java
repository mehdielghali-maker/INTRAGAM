package dz.gam.poste.versement.domain.port.in;

import dz.gam.poste.versement.domain.model.Versement;

import java.util.List;
import java.util.UUID;

/** Port d'entrée : consultation des versements déposés (suivi). */
public interface ConsulterVersementsUseCase {

    List<Versement> lister(FiltreVersement filtre);

    Versement obtenir(UUID id);

    /** Compteur de navigation : versements en cours côté BPM (toutes agences). */
    long compterEnCours();
}
