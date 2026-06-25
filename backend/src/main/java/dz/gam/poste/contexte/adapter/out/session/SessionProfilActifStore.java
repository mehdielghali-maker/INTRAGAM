package dz.gam.poste.contexte.adapter.out.session;

import dz.gam.poste.contexte.domain.port.out.ProfilActifStore;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Mémorise le profil SSO actif dans la session serveur (HttpSession) — mock de la connexion.
 * Hors requête web (tests, bus), pas de session : on retombe sur le profil par défaut.
 */
@Component
public class SessionProfilActifStore implements ProfilActifStore {

    private static final String ATTRIBUT = "poste.contexte.profilActif";

    @Override
    public Optional<String> profilActif() {
        ServletRequestAttributes attributs = attributsCourants();
        if (attributs == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((String) attributs.getRequest().getSession().getAttribute(ATTRIBUT));
    }

    @Override
    public void definir(String identifiantProfil) {
        ServletRequestAttributes attributs = attributsCourants();
        if (attributs == null) {
            return;
        }
        attributs.getRequest().getSession(true).setAttribute(ATTRIBUT, identifiantProfil);
    }

    private static ServletRequestAttributes attributsCourants() {
        RequestAttributes attributs = RequestContextHolder.getRequestAttributes();
        return attributs instanceof ServletRequestAttributes servlet ? servlet : null;
    }
}
