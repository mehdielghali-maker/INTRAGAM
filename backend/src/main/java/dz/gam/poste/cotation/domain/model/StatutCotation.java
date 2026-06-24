package dz.gam.poste.cotation.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'une demande de cotation (clé stable ; les libellés affichés sont
 * configurables, jamais figés en dur — voir {@code CotationProperties}).
 *
 * <pre>
 * BROUILLON → ENVOYEE → EN_COURS → A_FINALISER → AFFAIRE_GAGNEE
 *                                              ↘ SANS_SUITE
 * </pre>
 *
 * Sources des transitions : ENVOYEE = action agence (poste) ; EN_COURS = prise en
 * charge souscripteur (BPM/OneBase) ; A_FINALISER + AFFAIRE_GAGNEE = PROASSUR.
 */
public enum StatutCotation {

    BROUILLON,
    ENVOYEE,
    EN_COURS,
    A_FINALISER,
    AFFAIRE_GAGNEE,
    SANS_SUITE;

    private static final Map<StatutCotation, Set<StatutCotation>> TRANSITIONS = new EnumMap<>(StatutCotation.class);

    static {
        TRANSITIONS.put(BROUILLON, Set.of(ENVOYEE));
        TRANSITIONS.put(ENVOYEE, Set.of(EN_COURS));
        TRANSITIONS.put(EN_COURS, Set.of(A_FINALISER));
        TRANSITIONS.put(A_FINALISER, Set.of(AFFAIRE_GAGNEE, SANS_SUITE));
        TRANSITIONS.put(AFFAIRE_GAGNEE, Set.of());
        TRANSITIONS.put(SANS_SUITE, Set.of());
    }

    public Set<StatutCotation> prochainsStatuts() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    public boolean peutTransitionnerVers(StatutCotation cible) {
        return prochainsStatuts().contains(cible);
    }

    /** Statut final (positif ou négatif). */
    public boolean estTerminal() {
        return prochainsStatuts().isEmpty();
    }

    /** « Cotation en cours » au sens du compteur d'accueil : transmise et non clôturée. */
    public boolean estActive() {
        return this == ENVOYEE || this == EN_COURS || this == A_FINALISER;
    }
}
