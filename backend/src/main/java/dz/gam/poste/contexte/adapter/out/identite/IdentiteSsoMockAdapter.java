package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.model.Identite;
import dz.gam.poste.contexte.domain.model.Utilisateur;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;
import dz.gam.poste.contexte.domain.port.out.ProfilActifStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MOCK du fournisseur d'identité SSO (Microsoft/Entra ID). Résout le profil actif de la
 * session (ou le profil par défaut) parmi les profils configurés, et en dérive l'utilisateur
 * et son périmètre d'agences. Permet de simuler différents AGA/agents par session.
 */
@Component("contexteIdentiteSsoMockAdapter")
public class IdentiteSsoMockAdapter implements IdentitePort {

    private final ContexteProperties properties;
    private final ProfilActifStore profilActif;

    public IdentiteSsoMockAdapter(ContexteProperties properties, ProfilActifStore profilActif) {
        this.properties = properties;
        this.profilActif = profilActif;
    }

    @Override
    public Identite identiteCourante() {
        ContexteProperties.Profil p = properties.resoudre(profilActif.profilActif().orElse(null));
        Utilisateur utilisateur = new Utilisateur(p.identifiant(), p.nomAffiche(), p.profil());
        List<Agence> agences = p.agences().stream().map(a -> new Agence(a.code(), a.nom())).toList();
        return new Identite(utilisateur, agences);
    }
}
