package dz.gam.poste.contexte.domain.port.in;

import dz.gam.poste.contexte.domain.model.Principal;

import java.util.Optional;

/** Cas d'usage : consulter l'utilisateur authentifié de la session, ou le déconnecter. */
public interface ConsulterSessionUseCase {

    Optional<Principal> sessionCourante();

    void deconnecter();
}
