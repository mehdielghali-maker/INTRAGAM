package dz.gam.poste.versement.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Événement de domaine publié sur le bus à la soumission d'un versement au BPM.
 * Permet la boucle fermée (le BPM le consomme et renvoie les statuts) et toute réaction
 * d'autres contextes. Montant en DA.
 */
public record VersementBancaireDeposeEvent(
        String reference,
        String codeAgence,
        String moisSituation,
        BigDecimal montantVerse,
        LocalDate dateVersement,
        Instant dateDepot) {

    public VersementBancaireDeposeEvent {
        Objects.requireNonNull(reference, "référence obligatoire");
        Objects.requireNonNull(codeAgence, "code agence obligatoire");
        Objects.requireNonNull(moisSituation, "mois obligatoire");
        if (montantVerse == null || montantVerse.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
    }
}
