package dz.gam.poste.dpd.domain.model;

/**
 * État de règlement d'une échéance. Établi EN AMONT par rapprochement échéancier
 * (PROASSUR) × encaissements (Sage) ; le module ne fait que le refléter.
 */
public enum StatutReglement {
    /** Réglée (une date de règlement est disponible). */
    REGLEE,
    /** Échue et non réglée (alerte). */
    ECHUE,
    /** À échoir (date future). */
    A_ECHOIR
}
