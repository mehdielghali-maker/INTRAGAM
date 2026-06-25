package dz.gam.poste.contexte.domain.port.out;

import dz.gam.poste.contexte.domain.model.Identite;

/**
 * Port de sortie vers le fournisseur d'identité SSO (Microsoft/Entra ID). Fournit
 * l'utilisateur et son périmètre d'agences. Mock en dev ; remplaçable par l'extraction
 * réelle depuis le token (claims) sans toucher au domaine.
 */
public interface IdentitePort {

    Identite identiteCourante();
}
