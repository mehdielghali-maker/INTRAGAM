package dz.gam.poste.dpd.adapter.out.bpmmock;

import dz.gam.poste.dpd.domain.model.Validateur;
import dz.gam.poste.dpd.domain.port.out.BpmDpdPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MOCK BPM/OneBase : renvoie le validateur DR/central affecté à une demande. Affectation
 * déterministe (par hachage de la référence) pour la démo. À remplacer par l'API BPM réelle.
 */
@Component
public class BpmDpdMockAdapter implements BpmDpdPort {

    private static final List<String> VALIDATEURS = List.of("DR Centre — H. Brahimi", "Central — N. Lounis", "DR Est — F. Touati");

    @Override
    public Optional<Validateur> getValidateur(String noDemande) {
        if (noDemande == null || noDemande.isBlank()) {
            return Optional.empty();
        }
        String nom = VALIDATEURS.get(Math.floorMod(noDemande.hashCode(), VALIDATEURS.size()));
        return Optional.of(new Validateur(nom));
    }
}
