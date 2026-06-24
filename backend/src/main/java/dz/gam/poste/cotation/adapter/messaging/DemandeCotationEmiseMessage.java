package dz.gam.poste.cotation.adapter.messaging;

/**
 * Contrat de message « DemandeCotationEmise » (poste → central). DTO de transport à la
 * frontière, partagé avec le mock central qui le consomme.
 */
public record DemandeCotationEmiseMessage(
        String reference,
        String objet,
        String nomProspect,
        String numeroPolice,
        String commentaire,
        String codeAgence,
        String directionRegionale) {
}
