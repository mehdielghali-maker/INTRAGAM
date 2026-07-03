package dz.gam.poste.attestation.domain.model;

/**
 * Statut d'un relevé de production. Arbitrage validé : le relevé n'existe côté serveur
 * QU'À la validation (les ajouts de lignes avant validation vivent côté front) ; une fois
 * validé il est VERROUILLÉ — d'où un seul statut aujourd'hui. L'enum reste le point
 * d'extension si un jour un statut « transmis PROASSUR » s'ajoute (boucle fermée).
 */
public enum StatutReleve {
    VALIDEE
}
