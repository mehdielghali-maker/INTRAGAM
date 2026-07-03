package dz.gam.poste.attestation.domain.model;

/**
 * Résultat de la lecture OCR d'une attestation (contrat /lire-attestation FIGÉ, partagé
 * par les trois chantiers). Tous les champs texte sont nullables : un champ non lu vaut
 * {@code null}, jamais une chaîne vide « devinée ». Les formats attendus (police 15 chiffres,
 * quittance 8 chiffres, dates JJ/MM/AAAA, prime « 2400,97 », agence NN.AA.NNNN) sont produits
 * par le service OCR ; le domaine ne les re-devine pas.
 */
public record ResultatAttestation(
        String numeroPolice,     // 15 chiffres, ou null si non lu
        String numeroQuittance,  // 8 chiffres, ou null
        String immatriculation,  // ou null
        String assure,           // nom de l'assuré, ou null
        String valideDu,         // JJ/MM/AAAA, ou null
        String valideAu,         // JJ/MM/AAAA, ou null
        String primeTTC,         // ex. "2400,97", ou null
        String codeAgence,       // NN.AA.NNNN, ou null
        double confiance,        // [0..1]
        String statut,           // "lu" | "a_verifier"
        String texteBrut         // texte OCR complet (aide au contrôle visuel)
) {

    /** Lecture jugée fiable par l'OCR. */
    public static final String STATUT_LU = "lu";
    /** Lecture incertaine : l'AGA doit vérifier/corriger avant d'ajouter la ligne. */
    public static final String STATUT_A_VERIFIER = "a_verifier";

    /**
     * Résultat NEUTRE : renvoyé quand l'OCR est indisponible ou muet. Rien n'est lu,
     * confiance nulle, statut « à vérifier » — la saisie manuelle reste toujours possible,
     * la panne du service ne bloque JAMAIS la collecte.
     */
    public static ResultatAttestation neutre() {
        return new ResultatAttestation(null, null, null, null, null, null, null, null,
                0.0, STATUT_A_VERIFIER, "");
    }
}
