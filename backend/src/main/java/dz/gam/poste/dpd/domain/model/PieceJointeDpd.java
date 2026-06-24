package dz.gam.poste.dpd.domain.model;

import java.util.Objects;

/**
 * Référence d'une pièce jointe en GED OneBase, avec son type documentaire. Le poste ne
 * conserve que la référence (nom + id GED + type), jamais le binaire.
 */
public record PieceJointeDpd(TypePiece type, String nomFichier, String gedId) {

    public PieceJointeDpd {
        Objects.requireNonNull(type, "type obligatoire");
        Objects.requireNonNull(nomFichier, "nomFichier obligatoire");
        Objects.requireNonNull(gedId, "gedId obligatoire");
    }
}
