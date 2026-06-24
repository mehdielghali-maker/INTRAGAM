package dz.gam.poste.dpd.domain.model;

import java.util.Map;

/** Contexte de saisie : identité SSO, libellés de statut et niveau de validation (config). */
public record ContexteDpd(
        IdentiteDpd identite,
        Map<String, String> libellesStatut,
        String niveauValidation) {
}
