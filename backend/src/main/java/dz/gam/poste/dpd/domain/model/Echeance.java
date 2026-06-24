package dz.gam.poste.dpd.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Une échéance de l'échéancier validé (source PROASSUR). {@code statutReglement} et
 * {@code dateReglement} reflètent le rapprochement PROASSUR × Sage fait en amont.
 */
public record Echeance(
        int numero,
        LocalDate datePrevue,
        BigDecimal montant,
        StatutReglement statutReglement,
        LocalDate dateReglement) {

    public boolean estReglee() {
        return statutReglement == StatutReglement.REGLEE;
    }
}
