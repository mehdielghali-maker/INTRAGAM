package dz.gam.poste.cheque.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Montant d'un chèque, avec sa devise. Value object immuable.
 *
 * <p>Le poste ne fait aucun calcul comptable : il transporte ce montant tel que
 * PROASSUR l'a fourni. Aucune tarification, aucune écriture comptable ici.
 */
public record Montant(BigDecimal valeur, String devise) {

    public Montant {
        Objects.requireNonNull(valeur, "Le montant est obligatoire");
        Objects.requireNonNull(devise, "La devise est obligatoire");
        if (valeur.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
        if (devise.isBlank()) {
            throw new IllegalArgumentException("La devise est obligatoire");
        }
        devise = devise.trim().toUpperCase();
    }

    /** Montant en dinar algérien (devise par défaut du périmètre GAM). */
    public static Montant dinars(BigDecimal valeur) {
        return new Montant(valeur, "DZD");
    }
}
