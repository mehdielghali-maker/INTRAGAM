package dz.gam.poste.dpd.domain.model;

import java.util.Objects;

/** N° de demande DPD (ex. {@code DE-2026-0231}). Identité propre au poste, générée à l'envoi. */
public record ReferenceDpd(String valeur) {

    public ReferenceDpd {
        Objects.requireNonNull(valeur, "La référence DPD est obligatoire");
        if (valeur.isBlank()) {
            throw new IllegalArgumentException("La référence DPD ne peut pas être vide");
        }
        valeur = valeur.trim();
    }

    @Override
    public String toString() {
        return valeur;
    }
}
