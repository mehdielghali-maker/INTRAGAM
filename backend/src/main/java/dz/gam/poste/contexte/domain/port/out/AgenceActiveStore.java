package dz.gam.poste.contexte.domain.port.out;

import java.util.Optional;

/**
 * Port de sortie : mémorisation de l'agence active pour la session courante.
 * Implémenté par un adapter porté par la session serveur (HttpSession) — JAMAIS par un
 * stockage navigateur. Vide tant que l'utilisateur n'a pas explicitement changé d'agence
 * (le domaine retombe alors sur la première agence du périmètre).
 */
public interface AgenceActiveStore {

    Optional<String> codeActif();

    void definir(String codeAgence);
}
