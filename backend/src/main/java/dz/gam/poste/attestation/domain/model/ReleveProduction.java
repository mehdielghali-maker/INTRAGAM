package dz.gam.poste.attestation.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Agrégat « relevé de production » : le LOT d'attestations d'une AGENCE pour un MOIS,
 * figé à la validation. Invariants :
 * <ul>
 *   <li>mois au format AAAA-MM (regex, mois 01..12) ;</li>
 *   <li>agence obligatoire (dérivée du contexte serveur, jamais du front) ;</li>
 *   <li>au moins une ligne ; pas de doublon de numéro de police dans le lot ;</li>
 *   <li>référence DÉTERMINISTE {@code PROD-{mois}-{codeAgence}} : un seul relevé possible
 *       par (agence, mois) — la garde « déjà validé » du service s'appuie dessus ;</li>
 *   <li>statut {@link StatutReleve#VALIDEE} dès la soumission : le relevé est VERROUILLÉ
 *       (les ajouts se font côté front tant que rien n'est validé).</li>
 * </ul>
 */
public record ReleveProduction(
        String reference,
        String mois,
        String codeAgence,
        List<LigneAttestation> lignes,
        StatutReleve statut,
        Instant dateValidation
) {

    private static final Pattern MOIS_VALIDE = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    public ReleveProduction {
        if (mois == null || !MOIS_VALIDE.matcher(mois).matches()) {
            throw new IllegalArgumentException("Mois invalide : format attendu AAAA-MM.");
        }
        if (codeAgence == null || codeAgence.isBlank()) {
            throw new IllegalArgumentException("L'agence du relevé est obligatoire.");
        }
        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Un relevé doit contenir au moins une ligne.");
        }
        if (statut == null) {
            throw new IllegalArgumentException("Le statut du relevé est obligatoire.");
        }
        if (dateValidation == null) {
            throw new IllegalArgumentException("La date de validation est obligatoire.");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("La référence du relevé est obligatoire.");
        }
        // Copie immuable : le lot validé est verrouillé, personne ne peut y ajouter une ligne.
        lignes = List.copyOf(lignes);
        rejeterDoublonsDePolice(lignes);
    }

    /**
     * Construit le relevé VALIDÉ (statut VALIDEE, référence déterministe) à la soumission.
     * C'est l'unique voie de création côté métier ; le constructeur canonique ne sert
     * qu'à la reconstitution depuis la persistance.
     */
    public static ReleveProduction valider(String mois, String codeAgence,
                                           List<LigneAttestation> lignes, Instant dateValidation) {
        return new ReleveProduction(referencePour(mois, codeAgence), mois, codeAgence,
                lignes, StatutReleve.VALIDEE, dateValidation);
    }

    /** Référence déterministe {@code PROD-{mois}-{codeAgence}} : un lot par agence et par mois. */
    public static String referencePour(String mois, String codeAgence) {
        return "PROD-" + mois + "-" + codeAgence;
    }

    public int nombreLignes() {
        return lignes.size();
    }

    private static void rejeterDoublonsDePolice(List<LigneAttestation> lignes) {
        Set<String> vues = new HashSet<>();
        for (LigneAttestation ligne : lignes) {
            if (!vues.add(ligne.numeroPolice())) {
                throw new IllegalArgumentException(
                        "Doublon de numéro de police dans le relevé : " + ligne.numeroPolice());
            }
        }
    }
}
