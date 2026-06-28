package dz.gam.poste.reconnaissance.config;

import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort;
import dz.gam.poste.reconnaissance.domain.service.ReconnaissanceService;
import dz.gam.poste.reconnaissance.domain.service.VerificationPlaqueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du domaine « reconnaissance » : instancie les services Java purs en leur
 * injectant le port (mock ou http, choisi par reco.mode via @ConditionalOnProperty sur
 * les adapters) et la propriété de blocage anti-fraude (reco.bloque-non-conforme).
 */
@Configuration
public class ReconnaissanceBeansConfig {

    @Bean
    public VerificationPlaqueService verificationPlaqueService() {
        return new VerificationPlaqueService();
    }

    @Bean
    public ReconnaissanceService reconnaissanceService(
            ReconnaissancePort reconnaissance,
            VerificationPlaqueService verification,
            @Value("${reco.bloque-non-conforme:false}") boolean bloqueNonConforme) {
        return new ReconnaissanceService(reconnaissance, verification, bloqueNonConforme);
    }
}
