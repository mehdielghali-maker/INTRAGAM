package dz.gam.poste.cotation.domain.port.out;

import dz.gam.poste.cotation.domain.model.PieceJointe;

/**
 * Port de sortie vers la GED OneBase. Le dépôt d'un fichier renvoie une RÉFÉRENCE
 * (le poste ne stocke pas le binaire). Mock en dev, remplaçable par l'API OneBase.
 */
public interface GedOneBasePort {

    PieceJointe deposer(String nomFichier);
}
