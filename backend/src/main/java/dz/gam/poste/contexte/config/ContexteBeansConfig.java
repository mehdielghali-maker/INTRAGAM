package dz.gam.poste.contexte.config;

import dz.gam.poste.contexte.domain.port.out.AgenceActiveStore;
import dz.gam.poste.contexte.domain.port.out.CompteAdminStore;
import dz.gam.poste.contexte.domain.port.out.ComptesAgaStore;
import dz.gam.poste.contexte.domain.port.out.IdentitePort;
import dz.gam.poste.contexte.domain.port.out.MotDePasseEncodeur;
import dz.gam.poste.contexte.domain.port.out.ProfilActifStore;
import dz.gam.poste.contexte.domain.port.out.SessionAuthStore;
import dz.gam.poste.contexte.domain.service.AuthentificationService;
import dz.gam.poste.contexte.domain.service.ContexteAgenceService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du contexte d'agence et de l'authentification (briques transverses). Instancie les
 * services de domaine (Java pur) en leur injectant leurs ports. Un même bean sert tous les
 * ports d'entrée qu'il implémente.
 */
@Configuration
@EnableConfigurationProperties(ContexteProperties.class)
public class ContexteBeansConfig {

    @Bean
    public ContexteAgenceService contexteAgenceService(IdentitePort identitePort, AgenceActiveStore store) {
        return new ContexteAgenceService(identitePort, store);
    }

    @Bean
    public AuthentificationService authentificationService(CompteAdminStore compteAdmin,
                                                           ComptesAgaStore comptesAga,
                                                           MotDePasseEncodeur encodeur,
                                                           SessionAuthStore sessionAuth,
                                                           ProfilActifStore profilActif) {
        return new AuthentificationService(compteAdmin, comptesAga, encodeur, sessionAuth, profilActif);
    }
}
