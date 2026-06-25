package dz.gam.poste.versement.domain.model;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Mois de la situation financière à justifier (année + mois). Valeur canonique
 * {@code AAAA-MM} (ex. {@code 2026-06}) ; libellé fr pour l'affichage (ex. « Juin 2026 »).
 */
public record MoisSituation(int annee, int mois) {

    private static final Locale FR = Locale.FRENCH;

    public MoisSituation {
        if (mois < 1 || mois > 12) {
            throw new IllegalArgumentException("Mois invalide : " + mois);
        }
        if (annee < 2000 || annee > 2100) {
            throw new IllegalArgumentException("Année invalide : " + annee);
        }
    }

    /** Parse une valeur {@code AAAA-MM}. */
    public static MoisSituation depuis(String valeur) {
        if (valeur == null || !valeur.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Mois attendu au format AAAA-MM : " + valeur);
        }
        String[] parts = valeur.split("-");
        return new MoisSituation(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public String valeur() {
        return "%04d-%02d".formatted(annee, mois);
    }

    public String libelle() {
        String nom = Month.of(mois).getDisplayName(TextStyle.FULL, FR);
        return Character.toUpperCase(nom.charAt(0)) + nom.substring(1) + " " + annee;
    }
}
