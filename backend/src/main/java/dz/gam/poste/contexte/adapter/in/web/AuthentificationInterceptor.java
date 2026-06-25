package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.domain.model.Principal;
import dz.gam.poste.contexte.domain.model.TypePrincipal;
import dz.gam.poste.contexte.domain.port.in.ConsulterSessionUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Garde d'authentification côté back (exécutée AVANT {@link AccesModuleInterceptor}). Toute API
 * exige une session authentifiée (HTTP 401), sauf {@code /api/auth/**} (connexion, état, config).
 * Les API d'administration {@code /api/admin/**} exigent en plus le rôle ADMIN (HTTP 403). On ne
 * fait jamais confiance au seul masquage du menu : un AGA ne peut pas appeler l'admin via curl.
 */
@Component
public class AuthentificationInterceptor implements HandlerInterceptor {

    private final ConsulterSessionUseCase session;

    public AuthentificationInterceptor(ConsulterSessionUseCase session) {
        this.session = session;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/")) {
            return true; // connexion / état / config : toujours accessibles
        }
        Optional<Principal> principal = session.sessionCourante();
        if (principal.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise");
            return false;
        }
        if (uri.startsWith("/api/admin/") && principal.get().type() != TypePrincipal.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Réservé à l'administration");
            return false;
        }
        return true;
    }
}
