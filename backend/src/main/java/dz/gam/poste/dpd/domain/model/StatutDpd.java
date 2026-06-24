package dz.gam.poste.dpd.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'une demande de paiement différé (DPD). Clé stable ; libellés affichés
 * configurables (voir {@code DpdProperties}).
 *
 * <pre>
 * BROUILLON → ENVOYEE → EN_VALIDATION → ACCORDEE
 *                                    ↘ REFUSEE
 * </pre>
 *
 * Une fois ACCORDEE, l'exécution (honorée/retard/rompue) relève de « Suivi des
 * échéanciers » — non gérée ici. Sources : ENVOYEE = agence (poste) ; EN_VALIDATION =
 * BPM (validateur DR/central) ; ACCORDEE = PROASSUR (EcheancierValide) ; REFUSEE = central.
 */
public enum StatutDpd {

    BROUILLON,
    ENVOYEE,
    EN_VALIDATION,
    ACCORDEE,
    REFUSEE;

    private static final Map<StatutDpd, Set<StatutDpd>> TRANSITIONS = new EnumMap<>(StatutDpd.class);

    static {
        TRANSITIONS.put(BROUILLON, Set.of(ENVOYEE));
        TRANSITIONS.put(ENVOYEE, Set.of(EN_VALIDATION));
        TRANSITIONS.put(EN_VALIDATION, Set.of(ACCORDEE, REFUSEE));
        TRANSITIONS.put(ACCORDEE, Set.of());
        TRANSITIONS.put(REFUSEE, Set.of());
    }

    public Set<StatutDpd> prochainsStatuts() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    public boolean peutTransitionnerVers(StatutDpd cible) {
        return prochainsStatuts().contains(cible);
    }

    public boolean estTerminal() {
        return prochainsStatuts().isEmpty();
    }

    /** « En cours » au sens du compteur de nav : transmise et pas encore tranchée. */
    public boolean estActive() {
        return this == ENVOYEE || this == EN_VALIDATION;
    }
}
