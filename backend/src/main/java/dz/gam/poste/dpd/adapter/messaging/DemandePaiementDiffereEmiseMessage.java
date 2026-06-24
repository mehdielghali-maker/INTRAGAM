package dz.gam.poste.dpd.adapter.messaging;

import java.math.BigDecimal;

/** Contrat de message « DemandePaiementDiffereEmise » (poste → central). */
public record DemandePaiementDiffereEmiseMessage(
        String reference,
        String noProposition,
        String nomAssure,
        BigDecimal montantPrime,
        int dureeContratMois) {
}
