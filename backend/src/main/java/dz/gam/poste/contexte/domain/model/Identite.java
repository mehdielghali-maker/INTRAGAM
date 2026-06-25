package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Identité SSO résolue : l'utilisateur connecté et la liste des agences de son périmètre.
 * Le périmètre est la SEULE source de vérité des agences accessibles (sécurité) ; il est
 * revérifié côté back à chaque changement d'agence active. Mock en dev.
 */
public record Identite(Utilisateur utilisateur, List<Agence> agencesGerees) {

    public Identite {
        if (agencesGerees == null || agencesGerees.isEmpty()) {
            throw new IllegalArgumentException("le périmètre doit contenir au moins une agence");
        }
        agencesGerees = List.copyOf(agencesGerees);
    }
}
