package dz.gam.poste.cotation.domain.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Souscripteur du central qui prend en charge la demande. Donnée issue de l'API BPM
 * (OneBase) — système de référence. N'existe qu'à partir du statut « En cours ».
 */
public record Souscripteur(String nom) {

    public Souscripteur {
        Objects.requireNonNull(nom, "Le nom du souscripteur est obligatoire");
        if (nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du souscripteur ne peut pas être vide");
        }
        nom = nom.trim();
    }

    /** Initiales pour la pastille (ex. « K. Bensalem » → « KB »). */
    public String initiales() {
        return Arrays.stream(nom.split("[\\s.]+"))
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase())
                .limit(2)
                .collect(Collectors.joining());
    }
}
