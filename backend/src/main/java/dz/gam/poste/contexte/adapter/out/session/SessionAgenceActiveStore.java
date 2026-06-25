package dz.gam.poste.contexte.adapter.out.session;

import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Mémorise l'agence active dans la session serveur (HttpSession) — conformément à
 * l'exigence « état applicatif de session, PAS de stockage navigateur ». Lit/écrit la
 * session de la requête courante via {@link RequestContextHolder}. Hors d'une requête web
 * (tests, traitements de bus), il n'y a pas de session : on retombe silencieusement sur le
 * comportement par défaut (agence = première du périmètre).
 */
@Component
public class SessionAgenceActiveStore implements AgenceActiveStore {

    private static final String ATTRIBUT = "poste.contexte.agenceActive";

    @Override
    public Optional<String> codeActif() {
        ServletRequestAttributes attributs = attributsCourants();
        if (attributs == null) {
            return Optional.empty();
        }
        Object valeur = attributs.getRequest().getSession().getAttribute(ATTRIBUT);
        return Optional.ofNullable((String) valeur);
    }

    @Override
    public void definir(String codeAgence) {
        ServletRequestAttributes attributs = attributsCourants();
        if (attributs == null) {
            return; // hors requête web : aucune session à alimenter
        }
        attributs.getRequest().getSession(true).setAttribute(ATTRIBUT, codeAgence);
    }

    private static ServletRequestAttributes attributsCourants() {
        RequestAttributes attributs = RequestContextHolder.getRequestAttributes();
        return attributs instanceof ServletRequestAttributes servlet ? servlet : null;
    }
}
