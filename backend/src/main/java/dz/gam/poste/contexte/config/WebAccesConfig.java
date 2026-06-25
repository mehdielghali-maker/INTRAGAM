package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.adapter.in.web.AccesModuleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Enregistre le contrôle d'accès par module sur les API. */
@Configuration
public class WebAccesConfig implements WebMvcConfigurer {

    private final AccesModuleInterceptor accesModule;

    public WebAccesConfig(AccesModuleInterceptor accesModule) {
        this.accesModule = accesModule;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accesModule).addPathPatterns("/api/**");
    }
}
