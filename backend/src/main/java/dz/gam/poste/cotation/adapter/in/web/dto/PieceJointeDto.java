package dz.gam.poste.cotation.adapter.in.web.dto;

/** Référence de pièce jointe exposée à l'UI (nom + identifiant GED OneBase). */
public record PieceJointeDto(String nomFichier, String gedId) {
}
