package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Identité SSO résolue : l'utilisateur connecté et son périmètre d'agences (hiérarchie à 2
 * niveaux : agences + sous-agences). Le périmètre est la SEULE source de vérité des agences
 * accessibles (sécurité) ; il est revérifié côté back à chaque changement. Mock en dev.
 */
public record Identite(Utilisateur utilisateur, List<GroupeAgences> perimetre) {

    public Identite {
        if (perimetre == null || perimetre.isEmpty()) {
            throw new IllegalArgumentException("le périmètre doit contenir au moins une agence");
        }
        perimetre = List.copyOf(perimetre);
    }

    /** Toutes les unités d'action (sous-agences et agences autonomes) du périmètre, à plat. */
    public List<Agence> feuilles() {
        return perimetre.stream().flatMap(g -> g.feuilles().stream()).toList();
    }
}
