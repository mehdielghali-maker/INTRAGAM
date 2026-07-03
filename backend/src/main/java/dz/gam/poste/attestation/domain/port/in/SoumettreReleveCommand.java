package dz.gam.poste.attestation.domain.port.in;

import dz.gam.poste.attestation.domain.model.LigneAttestation;

import java.util.List;

/**
 * Commande de soumission d'un relevé : le mois et les lignes collectées. L'AGENCE n'y
 * figure PAS : elle est dérivée du contexte serveur ({@code agencePourAction()}), jamais
 * transmise par le front (sécurité, même règle que le versement).
 */
public record SoumettreReleveCommand(String mois, List<LigneAttestation> lignes) {
}
