package dz.gam.poste.attestation.domain.port.in;

/**
 * Cas d'usage : valider et soumettre le relevé de production du mois pour l'agence ACTIVE.
 * Gardes : action impossible en consolidé (409) ; relevé déjà validé pour (agence, mois)
 * → verrouillé (409). Après soumission, le lot est figé.
 */
public interface SoumettreReleveUseCase {

    RecuReleve soumettre(SoumettreReleveCommand commande);

    /** Accusé de soumission renvoyé au front (contrat FIGÉ de POST /api/attestations/releves). */
    record RecuReleve(String reference, String mois, String codeAgence, int nombreLignes, String message) {
    }
}
