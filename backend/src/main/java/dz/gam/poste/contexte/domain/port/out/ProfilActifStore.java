package dz.gam.poste.contexte.domain.port.out;

import java.util.Optional;

/**
 * Port de sortie : profil SSO actif de la session (mock de la connexion). Permet de simuler
 * différents AGA/agents et leurs périmètres d'agences, par session serveur — sans stockage
 * navigateur. Vide tant qu'aucun profil n'a été sélectionné (le profil par défaut s'applique).
 */
public interface ProfilActifStore {

    Optional<String> profilActif();

    void definir(String identifiantProfil);
}
