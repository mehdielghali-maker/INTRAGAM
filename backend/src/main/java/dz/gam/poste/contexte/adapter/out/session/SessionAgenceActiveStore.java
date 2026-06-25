package dz.gam.poste.contexte.adapter.out.session;

import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Optional;

/**
 * Mémorise l'agence active dans la session serveur (HttpSession via bean session-scoped) —
 * conformément à l'exigence « état applicatif de session, PAS de stockage navigateur ».
 * Au refresh, le contexte est rechargé depuis le serveur ; au logout, la session expire.
 */
@Component
@SessionScope
public class SessionAgenceActiveStore implements AgenceActiveStore {

    private String codeAgence;

    @Override
    public Optional<String> codeActif() {
        return Optional.ofNullable(codeAgence);
    }

    @Override
    public void definir(String codeAgence) {
        this.codeAgence = codeAgence;
    }
}
