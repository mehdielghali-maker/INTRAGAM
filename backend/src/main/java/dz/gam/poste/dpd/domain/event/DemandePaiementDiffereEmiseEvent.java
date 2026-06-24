package dz.gam.poste.dpd.domain.event;

import dz.gam.poste.dpd.domain.model.ReferenceDpd;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Événement de domaine émis quand l'agence envoie une demande DPD pour validation.
 * Publié sur le bus vers le central (BPM puis PROASSUR).
 */
public record DemandePaiementDiffereEmiseEvent(
        ReferenceDpd reference,
        String noProposition,
        String nomAssure,
        BigDecimal montantPrime,
        int dureeContratMois,
        Instant dateEmission) {

    public DemandePaiementDiffereEmiseEvent {
        Objects.requireNonNull(reference, "reference obligatoire");
        Objects.requireNonNull(dateEmission, "dateEmission obligatoire");
    }
}
