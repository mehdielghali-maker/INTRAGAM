package dz.gam.poste.versement.adapter.messaging;

/**
 * Contrat de message « VersementStatut » (BPM → poste). Porte le nouveau statut, la
 * référence BPM (à la prise en contrôle) et le motif (en cas de rejet). Champs non
 * concernés laissés à null.
 */
public record VersementStatutMessage(
        String reference,
        String statut,
        String referenceBpm,
        String motif) {
}
