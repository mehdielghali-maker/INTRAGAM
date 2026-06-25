package dz.gam.poste.dpd.adapter.out.identite;

import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.dpd.config.DpdProperties;
import dz.gam.poste.dpd.domain.model.IdentiteDpd;
import dz.gam.poste.dpd.domain.port.out.IdentiteDpdPort;
import org.springframework.stereotype.Component;

/**
 * MOCK SSO (Microsoft/Entra ID). Utilisateur/DR/mails de la config ; l'AGENCE (code + nom)
 * est dérivée du contexte d'agence actif (commutateur global). En mode consolidé, première
 * agence du périmètre (la création est bloquée côté contrôleur).
 */
@Component
public class IdentiteDpdMockAdapter implements IdentiteDpdPort {

    private final DpdProperties properties;
    private final AgenceCouranteQuery agenceCourante;

    public IdentiteDpdMockAdapter(DpdProperties properties, AgenceCouranteQuery agenceCourante) {
        this.properties = properties;
        this.agenceCourante = agenceCourante;
    }

    @Override
    public IdentiteDpd identiteCourante() {
        DpdProperties.Identite id = properties.identite();
        Agence active = agenceCourante.agencesActives().get(0);
        return new IdentiteDpd(id.utilisateur(), active.code(), id.mailAgence(), active.nom(),
                id.directionRegionale(), id.mailDirectionRegionale());
    }
}
