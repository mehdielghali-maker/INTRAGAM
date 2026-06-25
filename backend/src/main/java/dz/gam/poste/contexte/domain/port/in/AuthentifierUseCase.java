package dz.gam.poste.contexte.domain.port.in;

import dz.gam.poste.contexte.domain.model.Principal;

/**
 * Cas d'usage : authentifier un utilisateur (admin ou AGA) par login + mot de passe. En cas
 * de succès, ouvre la session ; sinon lève {@code IdentifiantsInvalidesException}.
 */
public interface AuthentifierUseCase {

    Principal connecter(String login, String motDePasse);
}
