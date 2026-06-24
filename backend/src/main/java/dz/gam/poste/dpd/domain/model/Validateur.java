package dz.gam.poste.dpd.domain.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/** Validateur DR/central qui prend en charge la demande. Donnée issue du BPM (OneBase). */
public record Validateur(String nom) {

    public Validateur {
        Objects.requireNonNull(nom, "Le nom du validateur est obligatoire");
        if (nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du validateur ne peut pas être vide");
        }
        nom = nom.trim();
    }

    public String initiales() {
        return Arrays.stream(nom.split("[\\s.]+"))
                .filter(p -> !p.isBlank())
                .map(p -> p.substring(0, 1).toUpperCase())
                .limit(2)
                .collect(Collectors.joining());
    }
}
