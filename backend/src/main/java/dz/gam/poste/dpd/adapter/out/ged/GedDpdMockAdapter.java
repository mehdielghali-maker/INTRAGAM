package dz.gam.poste.dpd.adapter.out.ged;

import dz.gam.poste.dpd.domain.model.PieceJointeDpd;
import dz.gam.poste.dpd.domain.model.TypePiece;
import dz.gam.poste.dpd.domain.port.out.GedDpdPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MOCK GED OneBase. « Dépose » le fichier (avec son type documentaire, ex. RC) et renvoie
 * une référence ; le binaire reste dans la GED, jamais dans le poste.
 */
@Component
public class GedDpdMockAdapter implements GedDpdPort {

    @Override
    public PieceJointeDpd deposer(TypePiece type, String nomFichier) {
        String gedId = "GED-" + type.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PieceJointeDpd(type, nomFichier, gedId);
    }
}
