package dz.gam.poste.contexte.domain.model;

/**
 * Levée quand une requête porte sur une agence hors du périmètre de l'utilisateur.
 * SÉCURITÉ : ne jamais faire confiance à l'agence envoyée par le front — elle est
 * revérifiée contre le périmètre SSO côté back. Traduite en HTTP 403.
 */
public class AgenceHorsPerimetreException extends RuntimeException {

    public AgenceHorsPerimetreException(String codeAgence) {
        super("Agence hors périmètre : " + codeAgence);
    }
}
