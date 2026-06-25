package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Nœud du périmètre : une agence et, éventuellement, ses sous-agences.
 *
 * <ul>
 *   <li>Sans sous-agence → agence <b>autonome</b> : elle est sa propre unité d'action (feuille).</li>
 *   <li>Avec sous-agences → <b>groupe</b> : les unités d'action sont les sous-agences ;
 *       l'agence parente ne sert qu'à la <b>vue consolidée</b> du groupe (lecture seule).</li>
 * </ul>
 */
public record GroupeAgences(String code, String nom, List<Agence> sousAgences) {

    public GroupeAgences {
        sousAgences = sousAgences == null ? List.of() : List.copyOf(sousAgences);
    }

    public boolean estGroupe() {
        return !sousAgences.isEmpty();
    }

    /** Agence parente vue comme une agence simple (utile à l'affichage). */
    public Agence enTantQueAgence() {
        return new Agence(code, nom);
    }

    /** Unités d'action couvertes : les sous-agences, ou l'agence elle-même si autonome. */
    public List<Agence> feuilles() {
        return estGroupe() ? sousAgences : List.of(enTantQueAgence());
    }
}
