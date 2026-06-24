package dz.gam.poste.dpd.adapter.messaging;

/**
 * Contrat de message « StatutDpd » (central → poste). {@code statut} ∈
 * {EN_VALIDATION, ACCORDEE, REFUSEE}. {@code codeAccord} renseigné à ACCORDEE,
 * {@code motif} à REFUSEE.
 */
public record StatutDpdMessage(String reference, String statut, String codeAccord, String motif) {
}
