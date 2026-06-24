package dz.gam.poste.cotation.domain.event;

import dz.gam.poste.cotation.domain.model.ReferenceDemande;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement de domaine produit quand l'agence ENVOIE une demande de cotation.
 * Publié sur le bus vers le central (sa publication est un détail d'infrastructure
 * assuré par un port de sortie). Le central (BPM puis PROASSUR) répondra par des
 * événements de statut entrants.
 */
public record DemandeCotationEmiseEvent(
        ReferenceDemande reference,
        String objet,
        String nomProspect,
        String numeroPolice,
        String commentaire,
        String codeAgence,
        String directionRegionale,
        Instant dateEmission) {

    public DemandeCotationEmiseEvent {
        Objects.requireNonNull(reference, "reference obligatoire");
        Objects.requireNonNull(dateEmission, "dateEmission obligatoire");
    }
}
