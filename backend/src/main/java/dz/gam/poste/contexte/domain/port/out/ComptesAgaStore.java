package dz.gam.poste.contexte.domain.port.out;

import dz.gam.poste.contexte.domain.model.IdentifiantsAga;

import java.util.Optional;

/**
 * Port de sortie : recherche des identifiants de connexion d'un profil AGA/agent par son
 * login. Utilisé par l'authentification ; n'expose que ce qui est nécessaire pour connecter.
 */
public interface ComptesAgaStore {

    Optional<IdentifiantsAga> trouverParLogin(String login);
}
