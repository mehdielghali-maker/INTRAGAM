package dz.gam.poste.contexte.domain.model;

import java.util.List;

/**
 * Identité SSO résolue : l'utilisateur connecté et la liste (plate) des agences de son
 * périmètre — une agence étant un point de vente. Le périmètre est la SEULE source de vérité
 * des agences accessibles (sécurité) ; il est revérifié côté back à chaque changement. Mock en dev.
 */
public record Identite(Utilisateur utilisateur, List<Agence> agencesGerees, List<String> modules) {

    public Identite {
        if (agencesGerees == null || agencesGerees.isEmpty()) {
            throw new IllegalArgumentException("le périmètre doit contenir au moins une agence");
        }
        agencesGerees = List.copyOf(agencesGerees);
        modules = modules == null ? List.of() : List.copyOf(modules);
    }
}
