package dz.gam.poste.cotation.domain.model;

import java.util.Objects;

/**
 * N° de demande de cotation (ex. {@code DC-2026-0412}). Identité propre au poste,
 * générée à l'envoi. Value object immuable.
 */
public record ReferenceDemande(String valeur) {

    public ReferenceDemande {
        Objects.requireNonNull(valeur, "La référence de demande est obligatoire");
        if (valeur.isBlank()) {
            throw new IllegalArgumentException("La référence de demande ne peut pas être vide");
        }
        valeur = valeur.trim();
    }

    @Override
    public String toString() {
        return valeur;
    }
}
