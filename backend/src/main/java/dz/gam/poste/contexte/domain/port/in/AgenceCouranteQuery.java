package dz.gam.poste.contexte.domain.port.in;

import dz.gam.poste.contexte.domain.model.Agence;

/**
 * Port d'entrée transverse : permet aux autres fonctions (accueil, versement, …) de
 * connaître l'agence active et d'exiger un accès dans le périmètre. Toute lecture de
 * données dérive l'agence du contexte serveur, jamais d'un paramètre transmis par le front.
 */
public interface AgenceCouranteQuery {

    /** Agence active de la session (première du périmètre par défaut). */
    Agence agenceActive();

    /** Rejette ({@code AgenceHorsPerimetreException}, 403) si le code n'est pas dans le périmètre. */
    void exigerAcces(String codeAgence);
}
