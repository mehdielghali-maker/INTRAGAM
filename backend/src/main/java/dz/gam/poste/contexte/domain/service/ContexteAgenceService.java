package dz.gam.poste.contexte.domain.service;

import dz.gam.poste.contexte.domain.model.ActionConsolideeInterditeException;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.model.AgenceHorsPerimetreException;
import dz.gam.poste.contexte.domain.model.ContexteAgence;
import dz.gam.poste.contexte.domain.model.GroupeAgences;
import dz.gam.poste.contexte.domain.model.Identite;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.contexte.domain.port.in.ChangerAgenceActiveUseCase;
import dz.gam.poste.contexte.domain.port.in.ConsulterContexteAgenceUseCase;
import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;

import java.util.List;

/**
 * Service applicatif du contexte d'agence. Java pur (aucune annotation framework).
 *
 * <p>La sélection courante (mémorisée en session) est un code : une sous-agence/agence
 * autonome (= 1 feuille), une agence parente (= ses sous-agences) ou {@code CONSOLIDE}
 * (= tout le périmètre). Une sélection couvrant <b>plusieurs feuilles</b> est une vue
 * d'ensemble en LECTURE SEULE ; {@link #agencePourAction()} y lève
 * {@link ActionConsolideeInterditeException}. Sécurité : la sélection est toujours
 * revérifiée contre le périmètre SSO.
 */
public class ContexteAgenceService
        implements ConsulterContexteAgenceUseCase, ChangerAgenceActiveUseCase, AgenceCouranteQuery {

    /** Sentinelle stockée en session pour la vue consolidée globale. */
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
        String selection = selectionCourante(identite);
        List<Agence> couvertes = couvertes(identite, selection);
        boolean lectureSeule = couvertes.size() > 1;
        Agence active = lectureSeule ? null : couvertes.get(0);
        return new ContexteAgence(
                identite.utilisateur(), active, selection, libelle(identite, selection, couvertes, lectureSeule),
                identite.perimetre(), identite.feuilles().size() > 1, lectureSeule);
    }

    @Override
    public ContexteAgence changer(String code) {
        Identite identite = identitePort.identiteCourante();
        if (!selectionValide(identite, code)) {
            throw new AgenceHorsPerimetreException(code);
        }
        store.definir(code);
        return contexte();
    }

    @Override
    public boolean estConsolide() {
        Identite identite = identitePort.identiteCourante();
        return couvertes(identite, selectionCourante(identite)).size() > 1;
    }

    @Override
    public Agence agencePourAction() {
        Identite identite = identitePort.identiteCourante();
        List<Agence> couvertes = couvertes(identite, selectionCourante(identite));
        if (couvertes.size() > 1) {
            throw new ActionConsolideeInterditeException();
        }
        return couvertes.get(0);
    }

    @Override
    public List<Agence> agencesActives() {
        Identite identite = identitePort.identiteCourante();
        return couvertes(identite, selectionCourante(identite));
    }

    @Override
    public void exigerAcces(String codeAgence) {
        Identite identite = identitePort.identiteCourante();
        boolean dansPerimetre = identite.feuilles().stream().anyMatch(a -> a.code().equals(codeAgence));
        if (!dansPerimetre) {
            throw new AgenceHorsPerimetreException(codeAgence);
        }
    }

    /** Sélection mémorisée si (toujours) valide, sinon la première feuille du périmètre. */
    private String selectionCourante(Identite identite) {
        return store.codeActif()
                .filter(code -> selectionValide(identite, code))
                .orElse(identite.feuilles().get(0).code());
    }

    private boolean selectionValide(Identite identite, String code) {
        if (CODE_CONSOLIDE.equals(code)) {
            return identite.feuilles().size() > 1;
        }
        boolean groupe = identite.perimetre().stream().anyMatch(g -> g.estGroupe() && g.code().equals(code));
        boolean feuille = identite.feuilles().stream().anyMatch(a -> a.code().equals(code));
        return groupe || feuille;
    }

    /** Feuilles couvertes par une sélection (1 pour une feuille, les enfants pour un groupe, tout pour consolidé). */
    private List<Agence> couvertes(Identite identite, String code) {
        if (CODE_CONSOLIDE.equals(code)) {
            return identite.feuilles();
        }
        return identite.perimetre().stream()
                .filter(g -> g.estGroupe() && g.code().equals(code))
                .findFirst()
                .map(GroupeAgences::feuilles)
                .orElseGet(() -> identite.feuilles().stream()
                        .filter(a -> a.code().equals(code))
                        .findFirst()
                        .map(List::of)
                        .orElseGet(() -> List.of(identite.feuilles().get(0))));
    }

    private String libelle(Identite identite, String code, List<Agence> couvertes, boolean lectureSeule) {
        if (CODE_CONSOLIDE.equals(code)) {
            return "Toutes mes agences (consolidé)";
        }
        if (lectureSeule) {
            String nom = identite.perimetre().stream()
                    .filter(g -> g.code().equals(code)).findFirst()
                    .map(GroupeAgences::nom).orElse(code);
            return nom + " (consolidé)";
        }
        return couvertes.get(0).nom();
    }
}
