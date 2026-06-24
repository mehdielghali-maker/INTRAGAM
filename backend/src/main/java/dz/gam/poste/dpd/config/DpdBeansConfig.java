package dz.gam.poste.dpd.config;

import dz.gam.poste.dpd.domain.port.out.AccordSuiviRepository;
import dz.gam.poste.dpd.domain.port.out.BpmDpdPort;
import dz.gam.poste.dpd.domain.port.out.DemandeDpdRepository;
import dz.gam.poste.dpd.domain.port.out.GedDpdPort;
import dz.gam.poste.dpd.domain.port.out.IdentiteDpdPort;
import dz.gam.poste.dpd.domain.port.out.ProassurDpdPort;
import dz.gam.poste.dpd.domain.port.out.PublicationDpdPort;
import dz.gam.poste.dpd.domain.service.DpdService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Câblage du contexte « Accords d'échéancier » (service de domaine + config). */
@Configuration
@EnableConfigurationProperties(DpdProperties.class)
public class DpdBeansConfig {

    @Bean
    public DpdService dpdService(DemandeDpdRepository demandes, AccordSuiviRepository accords,
                                 ProassurDpdPort proassur, BpmDpdPort bpm, GedDpdPort ged,
                                 IdentiteDpdPort identite, PublicationDpdPort publication,
                                 DpdProperties properties, Clock horloge) {
        return new DpdService(demandes, accords, proassur, bpm, ged, identite, publication, properties, horloge);
    }
}
