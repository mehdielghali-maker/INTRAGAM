package dz.gam.poste.dpd.adapter.in.web.dto;

import dz.gam.poste.dpd.domain.model.TypePiece;

/** Référence d'une pièce jointe exposée à l'UI. */
public record PieceDto(TypePiece type, String nomFichier, String gedId) {
}
