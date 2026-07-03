package dz.gam.poste.attestation.config;

import dz.gam.poste.attestation.domain.port.out.LectureDocumentPort;
import dz.gam.poste.attestation.domain.port.out.ProductionPort;
import dz.gam.poste.attestation.domain.port.out.ReleveProductionRepository;
import dz.gam.poste.attestation.domain.service.AttestationService;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage du domaine « attestations » : instancie le service Java pur en lui injectant les
 * ports (OCR mock ou http choisi par ocr.mode via @ConditionalOnProperty sur les adapters,
 * production mock — seam PROASSUR/Sage, persistance) et le contexte d'agence transverse.
 * Réutilise le bean {@link Clock} défini dans le contexte chèques.
 */
@Configuration
public class AttestationBeansConfig {

    @Bean
    public AttestationService attestationService(LectureDocumentPort lecture,
                                                 ProductionPort production,
                                                 ReleveProductionRepository repository,
                                                 AgenceCouranteQuery agenceCourante,
                                                 Clock horloge) {
        return new AttestationService(lecture, production, repository, agenceCourante, horloge);
    }
}
