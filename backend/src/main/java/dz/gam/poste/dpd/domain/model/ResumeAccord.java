package dz.gam.poste.dpd.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Résumé d'un accord (source PROASSUR), affiché en tête de l'onglet « Mise à jour DPD ». */
public record ResumeAccord(
        String noAccord,
        String assure,
        String policeOuProposition,
        BigDecimal montantPrime,
        String statut,
        Instant dateDerniereMaj) {
}
