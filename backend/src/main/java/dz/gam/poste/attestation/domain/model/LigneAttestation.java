package dz.gam.poste.attestation.domain.model;

import java.util.Set;

/**
 * Ligne d'un relevé de production : une attestation collectée (lue par OCR puis confirmée
 * ou corrigée par l'AGA). Le numéro de police est la clé de la ligne (obligatoire, dédoublonné
 * au niveau du relevé) ; les autres champs restent tels que collectés — le poste COLLECTE,
 * il ne recalcule pas la vérité PROASSUR (vérification par police : seam prévu, non branché).
 */
public record LigneAttestation(
        String numeroPolice,
        String numeroQuittance,
        String immatriculation,
        String assure,
        String valideDu,
        String valideAu,
        String primeTTC,
        String codeAgence,
        String statutLigne // "confirme" | "a_verifier"
) {

    /** Ligne confirmée par l'AGA (lecture fiable ou corrigée). */
    public static final String STATUT_CONFIRME = "confirme";
    /** Ligne conservée avec un doute : signalée comme telle dans le relevé. */
    public static final String STATUT_A_VERIFIER = "a_verifier";

    private static final Set<String> STATUTS_VALIDES = Set.of(STATUT_CONFIRME, STATUT_A_VERIFIER);

    public LigneAttestation {
        if (numeroPolice == null || numeroPolice.isBlank()) {
            throw new IllegalArgumentException("Le numéro de police est obligatoire sur chaque ligne.");
        }
        if (statutLigne == null || !STATUTS_VALIDES.contains(statutLigne)) {
            throw new IllegalArgumentException(
                    "Statut de ligne invalide : attendu « confirme » ou « a_verifier ».");
        }
    }
}
