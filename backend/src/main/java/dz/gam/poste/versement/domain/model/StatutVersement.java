package dz.gam.poste.versement.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'un versement bancaire suivi par le poste :
 * Brouillon → Déposé → En contrôle → Validé / Rejeté.
 *
 * <p>Le statut (hors brouillon) est porté par le BPM : le poste reflète, il ne décide pas
 * de la validation comptable. « Validé » et « Rejeté » sont terminaux.
 */
public enum StatutVersement {

    BROUILLON,
    DEPOSE,
    EN_CONTROLE,
    VALIDE,
    REJETE;

    private static final Map<StatutVersement, Set<StatutVersement>> TRANSITIONS = new EnumMap<>(StatutVersement.class);

    static {
        TRANSITIONS.put(BROUILLON, Set.of(DEPOSE));
        TRANSITIONS.put(DEPOSE, Set.of(EN_CONTROLE));
        TRANSITIONS.put(EN_CONTROLE, Set.of(VALIDE, REJETE));
        TRANSITIONS.put(VALIDE, Set.of());
        TRANSITIONS.put(REJETE, Set.of());
    }

    /** Statuts terminaux (décision BPM rendue). */
    public boolean estTerminal() {
        return prochainsStatuts().isEmpty();
    }

    /** En cours côté BPM : déposé ou en contrôle (sert le compteur de navigation). */
    public boolean estEnCours() {
        return this == DEPOSE || this == EN_CONTROLE;
    }

    public Set<StatutVersement> prochainsStatuts() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    public boolean peutTransitionnerVers(StatutVersement cible) {
        return prochainsStatuts().contains(cible);
    }
}
