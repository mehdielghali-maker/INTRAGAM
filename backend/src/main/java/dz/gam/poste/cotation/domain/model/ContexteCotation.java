package dz.gam.poste.cotation.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Contexte de saisie d'une demande : identité SSO (pré-remplissage agence/DR),
 * liste des branches/objets et libellés de statut — tous configurables (jamais en dur).
 */
public record ContexteCotation(
        IdentiteAgence identite,
        List<String> branches,
        Map<String, String> libellesStatut) {
}
