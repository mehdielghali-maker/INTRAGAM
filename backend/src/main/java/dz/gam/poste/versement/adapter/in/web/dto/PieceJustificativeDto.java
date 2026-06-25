package dz.gam.poste.versement.adapter.in.web.dto;

/** Vue REST d'une pièce (reçu) : nom de fichier + référence GED. */
public record PieceJustificativeDto(String nomFichier, String gedId) {
}
