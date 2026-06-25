package dz.gam.poste.versement.domain.port.out;

import dz.gam.poste.versement.domain.model.PieceJustificative;

/**
 * Port de sortie vers la GED OneBase. Le dépôt d'un fichier renvoie une RÉFÉRENCE
 * (le poste ne stocke pas le binaire). Mock en dev, remplaçable par l'API OneBase.
 */
public interface GedVersementPort {

    PieceJustificative deposer(String nomFichier);
}
