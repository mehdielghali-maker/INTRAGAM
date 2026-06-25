package dz.gam.poste.versement.config;

import dz.gam.poste.versement.domain.port.out.GedVersementPort;
import dz.gam.poste.versement.domain.port.out.ProductionEncaissePort;
import dz.gam.poste.versement.domain.port.out.PublicationVersementPort;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import dz.gam.poste.versement.domain.port.out.VersementsBanquePort;
import dz.gam.poste.versement.domain.service.VersementService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage du domaine « Versement bancaire » : instancie le service (Java pur) avec ses
 * adapters (ports) et la config. Réutilise le bean {@link Clock} défini dans le contexte chèques.
 */
@Configuration
@EnableConfigurationProperties(VersementProperties.class)
public class VersementBeansConfig {

    @Bean
    public VersementService versementService(VersementRepository repository,
                                             PublicationVersementPort publication,
                                             GedVersementPort ged,
                                             ProductionEncaissePort production,
                                             VersementsBanquePort banque,
                                             VersementProperties properties,
                                             Clock horloge) {
        return new VersementService(repository, publication, ged, production, banque, properties, horloge);
    }
}
