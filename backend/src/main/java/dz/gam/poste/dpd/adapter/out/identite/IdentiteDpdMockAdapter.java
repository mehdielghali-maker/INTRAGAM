package dz.gam.poste.dpd.adapter.out.identite;

import dz.gam.poste.dpd.config.DpdProperties;
import dz.gam.poste.dpd.domain.model.IdentiteDpd;
import dz.gam.poste.dpd.domain.port.out.IdentiteDpdPort;
import org.springframework.stereotype.Component;

/** MOCK SSO (Microsoft/Entra ID). Identité issue de la config en dev. */
@Component
public class IdentiteDpdMockAdapter implements IdentiteDpdPort {

    private final DpdProperties properties;

    public IdentiteDpdMockAdapter(DpdProperties properties) {
        this.properties = properties;
    }

    @Override
    public IdentiteDpd identiteCourante() {
        DpdProperties.Identite id = properties.identite();
        return new IdentiteDpd(id.utilisateur(), id.codeAgence(), id.mailAgence(), id.nomAgence(),
                id.directionRegionale(), id.mailDirectionRegionale());
    }
}
