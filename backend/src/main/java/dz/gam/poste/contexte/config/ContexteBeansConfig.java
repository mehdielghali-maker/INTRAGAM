package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;
import dz.gam.poste.contexte.domain.service.ContexteAgenceService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du contexte d'agence (brique transverse). Instancie le service de domaine (Java
 * pur) en lui injectant le port d'identité (mock SSO) et le store d'agence active (session).
 * Le même bean sert les trois ports d'entrée (lecture, changement, requête transverse).
 */
@Configuration
@EnableConfigurationProperties(ContexteProperties.class)
public class ContexteBeansConfig {

    @Bean
    public ContexteAgenceService contexteAgenceService(IdentitePort identitePort, AgenceActiveStore store) {
        return new ContexteAgenceService(identitePort, store);
    }
}
