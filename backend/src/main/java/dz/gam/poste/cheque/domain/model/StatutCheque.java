package dz.gam.poste.cheque.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'un chèque suivi par le poste :
 * Émis → Imprimé → Remis à l'agence → Remis au bénéficiaire → Encaissé / Retourné.
 *
 * <p>Les transitions autorisées sont déclarées ici, dans le domaine. C'est la règle
 * métier de référence du workflow ; elle ne dépend d'aucun framework.
 */
public enum StatutCheque {

    EMIS,
    IMPRIME,
    REMIS_AGENCE,
    REMIS_BENEFICIAIRE,
    ENCAISSE,
    RETOURNE;

    private static final Map<StatutCheque, Set<StatutCheque>> TRANSITIONS = new EnumMap<>(StatutCheque.class);

    static {
        TRANSITIONS.put(EMIS, Set.of(IMPRIME));
        TRANSITIONS.put(IMPRIME, Set.of(REMIS_AGENCE));
        TRANSITIONS.put(REMIS_AGENCE, Set.of(REMIS_BENEFICIAIRE));
        TRANSITIONS.put(REMIS_BENEFICIAIRE, Set.of(ENCAISSE, RETOURNE));
        // Statuts terminaux : aucune transition sortante.
        TRANSITIONS.put(ENCAISSE, Set.of());
        TRANSITIONS.put(RETOURNE, Set.of());
    }

    /** Statuts terminaux : ils déclenchent le write-back vers PROASSUR (boucle fermée). */
    public boolean estTerminal() {
        return prochainsStatuts().isEmpty();
    }

    /** Statuts directement atteignables depuis celui-ci. */
    public Set<StatutCheque> prochainsStatuts() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    /** Vrai si {@code cible} est une transition autorisée depuis ce statut. */
    public boolean peutTransitionnerVers(StatutCheque cible) {
        return prochainsStatuts().contains(cible);
    }
}
