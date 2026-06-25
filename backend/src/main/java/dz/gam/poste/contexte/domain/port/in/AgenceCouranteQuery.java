package dz.gam.poste.contexte.domain.port.in;

import dz.gam.poste.contexte.domain.model.Agence;

import java.util.List;

/**
 * Port d'entrée transverse : permet aux autres fonctions (accueil, versement, …) de
 * connaître l'agence active et d'exiger un accès dans le périmètre. Toute lecture/action
 * dérive l'agence du contexte serveur, jamais d'un paramètre transmis par le front.
 */
public interface AgenceCouranteQuery {

    /** Vrai si la vue consolidée (« Toutes mes agences ») est active. */
    boolean estConsolide();

    /**
     * Agence à utiliser pour une ACTION (création, dépôt…). En mode consolidé, une action
     * n'a pas de sens → {@code ActionConsolideeInterditeException} (HTTP 409).
     */
    Agence agencePourAction();

    /**
     * Agences concernées par une LECTURE : la seule agence active, ou tout le périmètre en
     * mode consolidé (vue d'ensemble).
     */
    List<Agence> agencesActives();

    /** Rejette ({@code AgenceHorsPerimetreException}, 403) si le code n'est pas dans le périmètre. */
    void exigerAcces(String codeAgence);
}
