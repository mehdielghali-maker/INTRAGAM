package dz.gam.poste.versement.domain.model;

import java.util.Objects;

/**
 * Référence d'une pièce (reçu de versement) stockée dans la GED OneBase. Le poste ne
 * conserve QUE la référence (nom + identifiant GED), jamais le binaire : pas de
 * duplication du document hors de sa GED propriétaire.
 */
public record PieceJustificative(String nomFichier, String gedId) {

    public PieceJustificative {
        Objects.requireNonNull(nomFichier, "Le nom du fichier est obligatoire");
        Objects.requireNonNull(gedId, "L'identifiant GED est obligatoire");
    }
}
