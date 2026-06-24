package dz.gam.poste.cotation.domain.port.out;

import dz.gam.poste.cotation.domain.model.IdentiteAgence;

/**
 * Port de sortie vers le fournisseur d'identité SSO (Microsoft/Entra ID).
 * Mock en dev ; remplaçable par l'extraction réelle depuis le token.
 */
public interface IdentiteAgencePort {

    IdentiteAgence identiteCourante();
}
