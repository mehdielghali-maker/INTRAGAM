package dz.gam.poste.versement.domain.port.out;

import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de sortie : persistance de l'état de suivi des versements (état du poste). */
public interface VersementRepository {

    Versement enregistrer(Versement versement);

    Optional<Versement> trouverParId(UUID id);

    Optional<Versement> trouverParReference(String reference);

    List<Versement> lister(FiltreVersement filtre);

    /** Nombre de versements en cours côté BPM (déposé ou en contrôle), toutes agences. */
    long compterEnCours();

    /** Nombre total de versements soumis (référencés) — amorce de la numérotation. */
    long compterReferences();
}
