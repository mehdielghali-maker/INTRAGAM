package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.model.GroupeAgences;
import dz.gam.poste.contexte.domain.model.Identite;
import dz.gam.poste.contexte.domain.model.Utilisateur;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MOCK du fournisseur d'identité SSO (Microsoft/Entra ID). En production, l'utilisateur et
 * son périmètre (agences + sous-agences) seraient extraits des claims du token. Ici, lus en
 * configuration.
 */
@Component("contexteIdentiteSsoMockAdapter")
public class IdentiteSsoMockAdapter implements IdentitePort {

    private final ContexteProperties properties;

    public IdentiteSsoMockAdapter(ContexteProperties properties) {
        this.properties = properties;
    }

    @Override
    public Identite identiteCourante() {
        ContexteProperties.Utilisateur u = properties.utilisateur();
        Utilisateur utilisateur = new Utilisateur(u.identifiant(), u.nomAffiche(), u.profil());
        List<GroupeAgences> perimetre = properties.agences().stream()
                .map(a -> new GroupeAgences(a.code(), a.nom(), sousAgences(a)))
                .toList();
        return new Identite(utilisateur, perimetre);
    }

    private static List<Agence> sousAgences(ContexteProperties.Agence agence) {
        if (agence.sousAgences() == null) {
            return List.of();
        }
        return agence.sousAgences().stream()
                .map(s -> new Agence(s.code(), s.nom()))
                .toList();
    }
}
