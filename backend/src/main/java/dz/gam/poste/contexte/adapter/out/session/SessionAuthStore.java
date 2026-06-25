package dz.gam.poste.contexte.adapter.out.session;

import dz.gam.poste.contexte.domain.model.Principal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Mémorise l'utilisateur authentifié dans la session serveur (HttpSession) — calqué sur
 * {@link SessionProfilActifStore}. Hors requête web (tests, bus), pas de session : vide.
 * {@link #effacer()} invalide la session (déconnexion) pour repartir d'une session neuve.
 */
@Component
public class SessionAuthStore implements dz.gam.poste.contexte.domain.port.out.SessionAuthStore {

    private static final String ATTRIBUT = "poste.auth.principal";

    @Override
    public Optional<Principal> principal() {
        ServletRequestAttributes attributs = attributsCourants();
        if (attributs == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((Principal) attributs.getRequest().getSession().getAttribute(ATTRIBUT));
    }

    @Override
    public void definir(Principal principal) {
        ServletRequestAttributes attributs = attributsCourants();
        if (attributs == null) {
            return;
        }
        attributs.getRequest().getSession(true).setAttribute(ATTRIBUT, principal);
    }

    @Override
    public void effacer() {
        ServletRequestAttributes attributs = attributsCourants();
        if (attributs == null) {
            return;
        }
        if (attributs.getRequest().getSession(false) != null) {
            attributs.getRequest().getSession(false).invalidate();
        }
    }

    private static ServletRequestAttributes attributsCourants() {
        RequestAttributes attributs = RequestContextHolder.getRequestAttributes();
        return attributs instanceof ServletRequestAttributes servlet ? servlet : null;
    }
}
