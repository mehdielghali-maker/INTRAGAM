package dz.gam.poste.tableaubord.config;

import dz.gam.poste.tableaubord.domain.port.out.CompteursAgencePort;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursProassurPort;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursSagePort;
import dz.gam.poste.tableaubord.domain.service.TableauBordService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage du contexte « accueil agence ». Instancie le service de domaine (Java pur) en
 * lui injectant les adapters (ports) et la config. Réutilise le bean {@link Clock} déjà
 * défini dans le contexte chèques.
 */
@Configuration
@EnableConfigurationProperties(TableauBordProperties.class)
public class TableauBordBeansConfig {

    @Bean
    public TableauBordService tableauBordService(IndicateursProassurPort proassur,
                                                 IndicateursSagePort sage,
                                                 CompteursAgencePort compteurs,
                                                 TableauBordProperties properties,
                                                 Clock horloge) {
        return new TableauBordService(proassur, sage, compteurs, properties, horloge);
    }
}
