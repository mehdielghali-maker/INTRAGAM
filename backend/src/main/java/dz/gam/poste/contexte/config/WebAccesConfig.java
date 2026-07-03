package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.adapter.in.web.AccesModuleInterceptor;
import dz.gam.poste.contexte.adapter.in.web.AuthentificationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enregistre les gardes sur les API : d'abord l'authentification (401/403), puis le contrôle
 * d'accès par module. L'ordre est significatif — l'authentification doit passer en premier.
 * Configure aussi le CORS pour un front hébergé sur une AUTRE origine (déploiement), via la
 * propriété {@code cors.allowed-origins} (env {@code CORS_ALLOWED_ORIGINS}). Vide = aucune origine
 * externe (même origine seulement — cas du dev avec proxy Vite et du compose avec nginx).
 */
@Configuration
public class WebAccesConfig implements WebMvcConfigurer {

    private final AuthentificationInterceptor authentification;
    private final AccesModuleInterceptor accesModule;

    /** Origines autorisées (séparées par des virgules) ; jamais en dur — par variable d'environnement. */
    @Value("${cors.allowed-origins:}")
    private String originesAutorisees;

    public WebAccesConfig(AuthentificationInterceptor authentification, AccesModuleInterceptor accesModule) {
        this.authentification = authentification;
        this.accesModule = accesModule;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authentification).addPathPatterns("/api/**");
        registry.addInterceptor(accesModule).addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (originesAutorisees == null || originesAutorisees.isBlank()) {
            return; // pas de CORS : front servi en même origine (proxy nginx / proxy Vite)
        }
        registry.addMapping("/api/**")
                // allowedOriginPatterns (et non allowedOrigins) : tolère « * » AVEC credentials —
                // Spring renvoie alors l'origine appelante au lieu de lever une erreur 500 sur
                // chaque requête cross-origin si l'exploitant met CORS_ALLOWED_ORIGINS=*.
                .allowedOriginPatterns(originesAutorisees.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true); // sessions (cookie) → préférer des origines explicites
    }
}
