package dz.gam.poste.versement.adapter.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Contrat de message « VersementBancaireDepose » (poste → BPM). DTO de transport à la
 * frontière, partagé avec le BPM (mock) qui le consomme.
 */
public record VersementDeposeMessage(
        String reference,
        String codeAgence,
        String moisSituation,
        BigDecimal montantVerse,
        LocalDate dateVersement,
        Instant dateDepot) {
}
