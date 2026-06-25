package dz.gam.poste.contexte.domain.port.in;

import dz.gam.poste.contexte.domain.model.ContexteAgence;

/** Port d'entrée : lecture du contexte d'agence (utilisateur, agence active, périmètre). */
public interface ConsulterContexteAgenceUseCase {

    ContexteAgence contexte();
}
