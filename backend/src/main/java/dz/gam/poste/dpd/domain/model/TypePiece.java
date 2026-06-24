package dz.gam.poste.dpd.domain.model;

/**
 * Type documentaire d'une pièce jointe dans la GED OneBase.
 * {@link #RC} : Registre de Commerce — pièce obligatoire, type dédié.
 * {@link #AUTRE} : pièce générale facultative.
 */
public enum TypePiece {
    RC,
    AUTRE
}
