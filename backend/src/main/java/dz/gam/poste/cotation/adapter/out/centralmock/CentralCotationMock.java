package dz.gam.poste.cotation.adapter.out.centralmock;

import dz.gam.poste.cotation.adapter.messaging.CotationStatutMessage;
import dz.gam.poste.cotation.adapter.messaging.DemandeCotationEmiseMessage;
import dz.gam.poste.cotation.config.CotationBus;
import dz.gam.poste.cotation.config.CotationProperties;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MOCK du central. Simule le traitement d'une demande reçue : prise en charge par un
 * souscripteur (source BPM/OneBase → statut EN_COURS) puis cotation faite dans PROASSUR
 * (source PROASSUR → statut A_FINALISER + n° proposition + réf. devis). « Affaire gagnée »
 * est déclenchée séparément à l'impression de la quittance (voir le contrôleur mock).
 *
 * <p>Remplaçable par les vraies intégrations BPM/PROASSUR sans toucher au domaine.
 * Délais configurables ({@code poste.cotation.mock.*}) — 0 en test.
 */
@Component
public class CentralCotationMock {

    private static final Logger log = LoggerFactory.getLogger(CentralCotationMock.class);
    private static final List<String> SOUSCRIPTEURS = List.of("K. Bensalem", "M. Cherif", "S. Haddad");

    private final RabbitTemplate rabbitTemplate;
    private final CotationProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "central-cotation-mock");
        t.setDaemon(true);
        return t;
    });

    public CentralCotationMock(RabbitTemplate rabbitTemplate, CotationProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @RabbitListener(queues = CotationBus.QUEUE_DEMANDE_EMISE)
    public void surDemandeEmise(DemandeCotationEmiseMessage message) {
        String reference = message.reference();
        String suffixe = reference.replaceAll("[^0-9]", "");
        String souscripteur = SOUSCRIPTEURS.get(Math.floorMod(reference.hashCode(), SOUSCRIPTEURS.size()));
        log.info("[MOCK CENTRAL] Demande reçue {} — affectation à {}", reference, souscripteur);

        long delaiPriseEnCharge = properties.mock().delaiPriseEnChargeMs();
        long delaiCotation = delaiPriseEnCharge + properties.mock().delaiCotationMs();

        // BPM/OneBase : prise en charge par un souscripteur → EN_COURS
        planifier(delaiPriseEnCharge, () -> publier(new CotationStatutMessage(
                reference, StatutCotation.EN_COURS.name(), souscripteur, null, null, null)));

        // PROASSUR : cotation faite → A_FINALISER + références (aucun contenu de devis)
        planifier(delaiCotation, () -> publier(new CotationStatutMessage(
                reference, StatutCotation.A_FINALISER.name(), null,
                "PR-" + suffixe, "DV-" + suffixe, null)));
    }

    private void planifier(long delaiMs, Runnable action) {
        scheduler.schedule(action, Math.max(0, delaiMs), TimeUnit.MILLISECONDS);
    }

    private void publier(CotationStatutMessage message) {
        rabbitTemplate.convertAndSend(CotationBus.EXCHANGE, CotationBus.RK_COTATION_STATUT, message);
        log.info("[MOCK CENTRAL] Statut renvoyé : référence={} statut={}", message.reference(), message.statut());
    }

    @PreDestroy
    void arret() {
        scheduler.shutdownNow();
    }
}
