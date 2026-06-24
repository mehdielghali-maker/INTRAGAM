package dz.gam.poste.cotation.domain.port.in;

import dz.gam.poste.cotation.domain.model.ContexteCotation;

/** Port d'entrée : contexte de saisie (identité SSO, branches, libellés de statut). */
public interface ConsulterContexteUseCase {

    ContexteCotation contexte();
}
