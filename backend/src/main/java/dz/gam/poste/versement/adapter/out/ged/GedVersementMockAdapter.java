package dz.gam.poste.versement.adapter.out.ged;

import dz.gam.poste.versement.domain.model.PieceJustificative;
import dz.gam.poste.versement.domain.port.out.GedVersementPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MOCK de la GED OneBase pour les reçus de versement. « Dépose » le fichier et renvoie une
 * référence GED ; le poste ne conserve que cette référence, jamais le binaire. À remplacer
 * par l'API OneBase réelle sans toucher au domaine.
 */
@Component
public class GedVersementMockAdapter implements GedVersementPort {

    @Override
    public PieceJustificative deposer(String nomFichier) {
        String gedId = "GED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PieceJustificative(nomFichier, gedId);
    }
}
