package dz.gam.poste.cotation.domain.model;

/**
 * Identité de l'utilisateur connecté et de son agence, issue du SSO (Microsoft/Entra ID).
 * En dev, fournie par un adapter mock. Remplace le bloc « Code Accès » du legacy.
 */
public record IdentiteAgence(String utilisateur, String codeAgence, String nomAgence, String directionRegionale) {
}
