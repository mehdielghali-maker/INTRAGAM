package dz.gam.poste.cheque.domain.model;

import java.util.Objects;

/**
 * Référence du chèque telle que PROASSUR la connaît. C'est la clé qui relie le
 * dossier de suivi du poste au règlement propriété de l'ERP.
 *
 * <p>Value object : immuable, comparé par valeur, validé à la construction.
 */
public record ReferenceCheque(String valeur) {

    public ReferenceCheque {
        Objects.requireNonNull(valeur, "La référence du chèque est obligatoire");
        if (valeur.isBlank()) {
            throw new IllegalArgumentException("La référence du chèque ne peut pas être vide");
        }
        valeur = valeur.trim();
    }

    @Override
    public String toString() {
        return valeur;
    }
}
