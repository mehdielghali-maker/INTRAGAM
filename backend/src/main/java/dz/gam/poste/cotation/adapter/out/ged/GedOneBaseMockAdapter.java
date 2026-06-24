package dz.gam.poste.cotation.adapter.out.ged;

import dz.gam.poste.cotation.domain.model.PieceJointe;
import dz.gam.poste.cotation.domain.port.out.GedOneBasePort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MOCK de la GED OneBase. « Dépose » le fichier et renvoie une référence GED. Le poste ne
 * conserve que cette référence — jamais le binaire (pas de duplication hors de la GED).
 * À remplacer par l'API OneBase réelle sans toucher au domaine.
 */
@Component
public class GedOneBaseMockAdapter implements GedOneBasePort {

    @Override
    public PieceJointe deposer(String nomFichier) {
        String gedId = "GED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PieceJointe(nomFichier, gedId);
    }
}
