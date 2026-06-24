package dz.gam.poste.dpd.domain.model;

/** Identité SSO (utilisateur + coordonnées agence/DR), pré-remplit les sections lecture seule. */
public record IdentiteDpd(
        String utilisateur,
        String codeAgence,
        String mailAgence,
        String nomAgence,
        String directionRegionale,
        String mailDirectionRegionale) {
}
