package dz.gam.poste.cotation.domain.model;

import java.util.Objects;

/**
 * Référence vers une pièce jointe stockée dans la GED OneBase (système de référence).
 * Le poste ne conserve QUE la référence (nom + identifiant GED), jamais le binaire :
 * pas de duplication du document hors de sa GED propriétaire.
 */
public record PieceJointe(String nomFichier, String gedId) {

    public PieceJointe {
        Objects.requireNonNull(nomFichier, "Le nom du fichier est obligatoire");
        Objects.requireNonNull(gedId, "L'identifiant GED est obligatoire");
    }
}
