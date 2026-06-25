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
 * session (ou le profil par défaut) parmi les profils PERSISTÉS (gérés via l'admin), et en
 * dérive l'utilisateur et son périmètre d'agences.
 */
@Component("contexteIdentiteSsoMockAdapter")
public class IdentiteSsoMockAdapter implements IdentitePort {

    private final ContexteProperties properties;
    private final ProfilActifStore profilActif;
    private final ProfilsAdminStore profils;

    public IdentiteSsoMockAdapter(ContexteProperties properties, ProfilActifStore profilActif,
                                  ProfilsAdminStore profils) {
        this.properties = properties;
        this.profilActif = profilActif;
        this.profils = profils;
    }

    @Override
    public Identite identiteCourante() {
        ContexteProperties.Profil p = profilActif.profilActif().flatMap(profils::trouver)
                .or(() -> profils.trouver(properties.profilDefaut()))
                .or(() -> profils.lister().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("Aucun profil SSO configuré"));
        Utilisateur utilisateur = new Utilisateur(p.identifiant(), p.nomAffiche(), p.profil());
        List<Agence> agences = p.agences().stream().map(a -> new Agence(a.code(), a.nom())).toList();
        return new Identite(utilisateur, agences);
    }
}
