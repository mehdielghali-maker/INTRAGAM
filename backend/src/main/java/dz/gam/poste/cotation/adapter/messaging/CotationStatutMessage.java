package dz.gam.poste.cotation.adapter.messaging;

/**
 * Contrat de message « CotationStatut » (central → poste). Porte le nouveau statut et,
 * selon le cas, le souscripteur (source BPM/OneBase) ou les références PROASSUR
 * (n° proposition, réf. devis, n° police). Champs non concernés laissés à null.
 */
public record CotationStatutMessage(
        String reference,
        String statut,
        String souscripteur,
        String numeroProposition,
        String referenceDevis,
        String numeroPolice) {
}
