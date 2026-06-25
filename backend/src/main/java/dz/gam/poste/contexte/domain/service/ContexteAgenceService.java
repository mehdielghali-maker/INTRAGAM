package dz.gam.poste.contexte.domain.service;

import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.model.AgenceHorsPerimetreException;
import dz.gam.poste.contexte.domain.model.ContexteAgence;
import dz.gam.poste.contexte.domain.model.Identite;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.contexte.domain.port.in.ChangerAgenceActiveUseCase;
import dz.gam.poste.contexte.domain.port.in.ConsulterContexteAgenceUseCase;
import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;

import java.util.Optional;

/**
 * Service applicatif du contexte d'agence. Java pur (aucune annotation framework).
 *
 * <p>Règle de sécurité centrale : l'agence active est TOUJOURS choisie dans le périmètre
 * de l'identité SSO. Le changement d'agence revérifie le code reçu contre ce périmètre ;
 * un code hors périmètre est rejeté. La vue consolidée n'est pas encore disponible.
 */
public class ContexteAgenceService
        implements ConsulterContexteAgenceUseCase, ChangerAgenceActiveUseCase, AgenceCouranteQuery {

    private static final boolean CONSOLIDE_DISPONIBLE = false;

    private final IdentitePort identitePort;
    private final AgenceActiveStore store;

    public ContexteAgenceService(IdentitePort identitePort, AgenceActiveStore store) {
        this.identitePort = identitePort;
        this.store = store;
    }

    @Override
    public ContexteAgence contexte() {
        Identite identite = identitePort.identiteCourante();
        return new ContexteAgence(identite.utilisateur(), agenceActive(identite),
                identite.agencesGerees(), CONSOLIDE_DISPONIBLE);
    }

    @Override
    public ContexteAgence changer(String codeAgence) {
        Identite identite = identitePort.identiteCourante();
        Agence cible = trouver(identite, codeAgence)
                .orElseThrow(() -> new AgenceHorsPerimetreException(codeAgence));
        store.definir(cible.code());
        return new ContexteAgence(identite.utilisateur(), cible,
                identite.agencesGerees(), CONSOLIDE_DISPONIBLE);
    }

    @Override
    public Agence agenceActive() {
        return agenceActive(identitePort.identiteCourante());
    }

    @Override
    public void exigerAcces(String codeAgence) {
        if (trouver(identitePort.identiteCourante(), codeAgence).isEmpty()) {
            throw new AgenceHorsPerimetreException(codeAgence);
        }
    }

    /** Agence active = celle mémorisée si elle est (toujours) dans le périmètre, sinon la première. */
    private Agence agenceActive(Identite identite) {
        return store.codeActif()
                .flatMap(code -> trouver(identite, code))
                .orElse(identite.agencesGerees().get(0));
    }

    private Optional<Agence> trouver(Identite identite, String codeAgence) {
        return identite.agencesGerees().stream()
                .filter(agence -> agence.code().equals(codeAgence))
                .findFirst();
    }
}
