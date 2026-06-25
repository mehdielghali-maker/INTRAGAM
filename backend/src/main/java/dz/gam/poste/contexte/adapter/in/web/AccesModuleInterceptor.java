package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.domain.port.in.ConsulterContexteAgenceUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Applique le contrôle d'accès par MODULE côté back : si le profil actif n'a pas le module
 * correspondant à l'API appelée, la requête est rejetée (HTTP 403). On ne fait jamais
 * confiance au seul masquage du menu côté front. L'accueil et les API techniques (contexte,
 * admin, mocks) ne sont pas restreints.
 */
@Component
public class AccesModuleInterceptor implements HandlerInterceptor {

    private final ConsulterContexteAgenceUseCase contexte;

    public AccesModuleInterceptor(ConsulterContexteAgenceUseCase contexte) {
        this.contexte = contexte;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String module = moduleDe(request.getRequestURI());
        if (module == null) {
            return true; // API non rattachée à un module restreint
        }
        List<String> autorises = contexte.contexte().modules();
        if (autorises.contains(module)) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Module non autorisé : " + module);
        return false;
    }

    /** Rattache un préfixe d'API à un id de module (cf. Modules). Null = non restreint. */
    private static String moduleDe(String uri) {
        if (uri.startsWith("/api/cheques")) {
            return "cheques";
        }
        if (uri.startsWith("/api/cotation")) {
            return "cotation";
        }
        if (uri.startsWith("/api/dpd")) {
            return "accords";
        }
        if (uri.startsWith("/api/versement")) {
            return "versement";
        }
        return null;
    }
}
