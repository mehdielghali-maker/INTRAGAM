package dz.gam.poste.dpd.domain.port.in;

import dz.gam.poste.dpd.domain.model.TypePersonne;

import java.util.List;

/**
 * Données saisies pour une demande DPD. La souscription est rechargée côté serveur depuis
 * {@code noProposition} (autoritaire, PROASSUR) ; l'agence/DR vient du SSO. Les pièces sont
 * des noms de fichiers déposés en GED, séparés par type (RC vs autres).
 */
public record CreerDpdCommand(
        String noProposition,
        boolean avenant,
        String commentaire,
        // Information client (pré-remplie depuis PROASSUR, éventuellement éditée)
        String nomAssure,
        String nomSouscripteur,
        String telephone,
        String cnrc,
        TypePersonne typePersonne,
        boolean institutionPublique,
        String adresse,
        // Pièces jointes (noms de fichiers déposés en GED)
        List<String> fichiersRc,
        List<String> fichiersAutres) {
}
