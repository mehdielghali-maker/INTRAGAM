package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.model.Validateur;

import java.util.Optional;

/** Port de sortie vers le BPM/OneBase : statut courant + validateur affecté. */
public interface BpmDpdPort {

    /** Validateur DR/central affecté à la demande ; vide tant que « non affecté ». */
    Optional<Validateur> getValidateur(String noDemande);
}
