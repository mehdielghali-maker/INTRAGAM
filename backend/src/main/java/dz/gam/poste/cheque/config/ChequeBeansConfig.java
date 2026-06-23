package dz.gam.poste.cheque.config;

import dz.gam.poste.cheque.domain.port.out.DossierChequeRepository;
import dz.gam.poste.cheque.domain.port.out.PublicationEvenementPort;
import dz.gam.poste.cheque.domain.service.ChequeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage du domaine. C'est le SEUL endroit qui connaît à la fois le domaine et Spring :
 * il instancie le service de domaine (Java pur) en lui injectant les adapters (ports).
 * Le service implémente les trois use cases ; le même bean est donc injecté partout où
 * une de ces interfaces est requise (contrôleur, listener).
 */
@Configuration
public class ChequeBeansConfig {

    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }

    @Bean
    public ChequeService chequeService(DossierChequeRepository repository,
                                       PublicationEvenementPort publication,
                                       Clock horloge) {
        return new ChequeService(repository, publication, horloge);
    }
}
