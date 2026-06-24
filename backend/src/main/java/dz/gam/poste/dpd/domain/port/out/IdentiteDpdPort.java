package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.model.IdentiteDpd;

/** Port de sortie vers le fournisseur d'identité SSO (mock en dev). */
public interface IdentiteDpdPort {

    IdentiteDpd identiteCourante();
}
