package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.adapter.in.web.AccesModuleInterceptor;
import dz.gam.poste.contexte.adapter.in.web.AuthentificationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enregistre les gardes sur les API : d'abord l'authentification (401/403), puis le contrôle
 * d'accès par module. L'ordre est significatif — l'authentification doit passer en premier.
 */
@Configuration
public class WebAccesConfig implements WebMvcConfigurer {

    private final AuthentificationInterceptor authentification;
    private final AccesModuleInterceptor accesModule;

    public WebAccesConfig(AuthentificationInterceptor authentification, AccesModuleInterceptor accesModule) {
        this.authentification = authentification;
        this.accesModule = accesModule;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authentification).addPathPatterns("/api/**");
        registry.addInterceptor(accesModule).addPathPatterns("/api/**");
    }
}
