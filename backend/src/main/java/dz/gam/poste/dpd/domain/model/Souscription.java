package dz.gam.poste.dpd.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Données de souscription, LECTURE SEULE, propriété de PROASSUR. Pré-remplies depuis la
 * proposition. Le poste les transporte pour affichage ; il ne les recalcule pas.
 */
public record Souscription(
        String noProposition,
        LocalDate dateProposition,
        String devisCreePar,
        BigDecimal montantPrime,
        String branche,
        LocalDate dateEffet,
        LocalDate dateEcheance,
        int dureeContratMois,
        String noPolice) {
}
