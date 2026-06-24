package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.model.PieceJointeDpd;
import dz.gam.poste.dpd.domain.model.TypePiece;

/**
 * Port de sortie vers la GED OneBase. Dépose un fichier (avec son type documentaire) et
 * renvoie la référence ; le binaire reste dans la GED, jamais dans le poste.
 */
public interface GedDpdPort {

    PieceJointeDpd deposer(TypePiece type, String nomFichier);
}
