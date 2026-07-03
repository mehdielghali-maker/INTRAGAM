package dz.gam.poste.attestation.domain.model;

/**
 * Levée quand un relevé de production a DÉJÀ été validé pour le couple (agence, mois) :
 * le lot est verrouillé, on ne re-soumet pas. Traduite en HTTP 409 (conflit) par
 * l'adapter web — le front propose alors la consultation du relevé existant.
 */
public class ReleveDejaValideException extends RuntimeException {

    public ReleveDejaValideException(String codeAgence, String mois) {
        super("Relevé déjà validé pour l'agence " + codeAgence + " et le mois " + mois
                + " : le lot est verrouillé.");
    }
}
