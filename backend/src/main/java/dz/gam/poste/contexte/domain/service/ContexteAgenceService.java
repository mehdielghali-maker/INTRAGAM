package dz.gam.poste.contexte.domain.service;

import dz.gam.poste.contexte.domain.model.ActionConsolideeInterditeException;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.model.AgenceHorsPerimetreException;
import dz.gam.poste.contexte.domain.model.ContexteAgence;
import dz.gam.poste.contexte.domain.model.Identite;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.contexte.domain.port.in.ChangerAgenceActiveUseCase;
import dz.gam.poste.contexte.domain.port.in.ConsulterContexteAgenceUseCase;
import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;

import java.util.List;
import java.util.Optional;

/**
 * Service applicatif du contexte d'agence. Java pur (aucune annotation framework).
 *
 * <p>La sélection (mémorisée en session) est soit une agence du périmètre, soit
 * {@code CONSOLIDE} (« Toutes mes agences »). Le mode consolidé est une vue d'ensemble en
 * LECTURE SEULE : {@link #agencePourAction()} y lève {@link ActionConsolideeInterditeException}.
 * Sécurité : la sélection est toujours revérifiée contre le périmètre SSO.
 */
public class ContexteAgenceService
        implements ConsulterContexteAgenceUseCase, ChangerAgenceActiveUseCase, AgenceCouranteQuery {

    /** Sentinelle stockée en session pour la vue consolidée. */
    public static final String CODE_CONSOLIDE = "CONSOLIDE";

    private final IdentitePort identitePort;
    private final AgenceActiveStore store;

    public ContexteAgenceService(IdentitePort identitePort, AgenceActiveStore store) {
        this.identitePort = identitePort;
        this.store = store;
    }

    @Override
    public ContexteAgence contexte() {
        Identite identite = identitePort.identiteCourante();
        boolean consolide = estConsolide(identite);
        Agence active = consolide ? null : agenceResolue(identite);
        return new ContexteAgence(identite.utilisateur(), active, identite.agencesGerees(),
                identite.agencesGerees().size() > 1, consolide);
    }

    @Override
    public ContexteAgence changer(String codeAgence) {
        Identite identite = identitePort.identiteCourante();
        if (CODE_CONSOLIDE.equals(codeAgence)) {
            if (identite.agencesGerees().size() <= 1) {
                throw new AgenceHorsPerimetreException(codeAgence); // consolidé non pertinent en mono-agence
            }
            store.definir(CODE_CONSOLIDE);
        } else {
            Agence cible = trouver(identite, codeAgence)
                    .orElseThrow(() -> new AgenceHorsPerimetreException(codeAgence));
            store.definir(cible.code());
        }
        return contexte();
    }

    @Override
    public boolean estConsolide() {
        return estConsolide(identitePort.identiteCourante());
    }

    @Override
    public Agence agencePourAction() {
        Identite identite = identitePort.identiteCourante();
        if (estConsolide(identite)) {
            throw new ActionConsolideeInterditeException();
        }
        return agenceResolue(identite);
    }

    @Override
    public List<Agence> agencesActives() {
        Identite identite = identitePort.identiteCourante();
        return estConsolide(identite) ? identite.agencesGerees() : List.of(agenceResolue(identite));
    }

    @Override
    public void exigerAcces(String codeAgence) {
        if (trouver(identitePort.identiteCourante(), codeAgence).isEmpty()) {
            throw new AgenceHorsPerimetreException(codeAgence);
        }
    }

    private boolean estConsolide(Identite identite) {
        return store.codeActif().filter(CODE_CONSOLIDE::equals).isPresent()
                && identite.agencesGerees().size() > 1;
    }

    /** Agence active = celle mémorisée si elle est (toujours) dans le périmètre, sinon la première. */
    private Agence agenceResolue(Identite identite) {
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
