package dz.gam.poste.tableaubord.adapter.out.proassurmock;

import java.util.Map;

/**
 * Facteur d'échelle des montants fictifs par agence (MOCK uniquement). Permet au
 * commutateur d'agence de faire varier visiblement les chiffres du tableau de bord, sans
 * impacter le domaine. L'agence de référence de la maquette ({@code 02.1.S.BENZERGA})
 * vaut 1,0 ; les autres sont plus petites. Disparaît avec le branchement réel PROASSUR/Sage.
 */
public final class FacteurAgenceMock {

    private static final Map<String, Double> FACTEURS = Map.of(
            "02.1.S.BENZERGA", 1.0,
            "02.4.Aïssat Idir", 0.6,
            "02.7.Draria", 0.4);

    private FacteurAgenceMock() {
    }

    public static double pour(String codeAgence) {
        return FACTEURS.getOrDefault(codeAgence, 1.0);
    }
}
