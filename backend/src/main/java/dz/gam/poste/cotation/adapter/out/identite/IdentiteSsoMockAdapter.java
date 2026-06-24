package dz.gam.poste.cotation.adapter.out.identite;

import dz.gam.poste.cotation.config.CotationProperties;
import dz.gam.poste.cotation.domain.model.IdentiteAgence;
import dz.gam.poste.cotation.domain.port.out.IdentiteAgencePort;
import org.springframework.stereotype.Component;

/**
 * MOCK du fournisseur d'identité SSO (Microsoft/Entra ID). En production, l'identité et
 * l'agence seraient extraites du token de l'utilisateur connecté. Ici, valeurs de config.
 */
@Component
public class IdentiteSsoMockAdapter implements IdentiteAgencePort {

    private final CotationProperties properties;

    public IdentiteSsoMockAdapter(CotationProperties properties) {
        this.properties = properties;
    }

    @Override
    public IdentiteAgence identiteCourante() {
        CotationProperties.Identite id = properties.identite();
        return new IdentiteAgence(id.utilisateur(), id.codeAgence(), id.nomAgence(), id.directionRegionale());
    }
}
