package dz.gam.poste.cotation.config;

import dz.gam.poste.cotation.domain.port.out.DemandeCotationRepository;
import dz.gam.poste.cotation.domain.port.out.GedOneBasePort;
import dz.gam.poste.cotation.domain.port.out.IdentiteAgencePort;
import dz.gam.poste.cotation.domain.port.out.PublicationCotationPort;
import dz.gam.poste.cotation.domain.service.CotationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Câblage du contexte « Demande de cotation » (service de domaine + config). */
@Configuration
@EnableConfigurationProperties(CotationProperties.class)
public class CotationBeansConfig {

    @Bean
    public CotationService cotationService(DemandeCotationRepository repository,
                                           PublicationCotationPort publication,
                                           IdentiteAgencePort identite,
                                           GedOneBasePort ged,
                                           CotationProperties properties,
                                           Clock horloge) {
        return new CotationService(repository, publication, identite, ged, properties, horloge);
    }
}
