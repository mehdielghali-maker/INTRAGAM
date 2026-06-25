package dz.gam.poste.cotation.adapter.out.identite;

import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.cotation.config.CotationProperties;
import dz.gam.poste.cotation.domain.model.IdentiteAgence;
import dz.gam.poste.cotation.domain.port.out.IdentiteAgencePort;
import org.springframework.stereotype.Component;

/**
 * MOCK du fournisseur d'identité SSO (Microsoft/Entra ID). L'utilisateur et la direction
 * régionale viennent de la config ; l'AGENCE (code + nom) est dérivée du contexte d'agence
 * actif (commutateur global), pour que la demande soit rattachée à l'agence choisie en haut.
 * En mode consolidé, on retombe sur la première agence du périmètre (la création est de
 * toute façon bloquée côté contrôleur).
 */
@Component
public class IdentiteSsoMockAdapter implements IdentiteAgencePort {

    private final CotationProperties properties;
    private final AgenceCouranteQuery agenceCourante;

    public IdentiteSsoMockAdapter(CotationProperties properties, AgenceCouranteQuery agenceCourante) {
        this.properties = properties;
        this.agenceCourante = agenceCourante;
    }

    @Override
    public IdentiteAgence identiteCourante() {
        CotationProperties.Identite id = properties.identite();
        Agence active = agenceCourante.agencesActives().get(0);
        return new IdentiteAgence(id.utilisateur(), active.code(), active.nom(), id.directionRegionale());
    }
}
