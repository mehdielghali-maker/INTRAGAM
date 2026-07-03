package dz.gam.poste.versement.adapter.in.messaging;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.model.PieceJustificative;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Sème deux versements bancaires FICTIFS (état DÉPOSÉ) par agence en réaction à
 * {@link AgencesDeclareesEvent} — au démarrage et à l'ajout d'une agence via l'admin — afin
 * que le « Suivi des versements » soit peuplé pour la démo de l'AGA.
 *
 * <p>Comme ses jumeaux (cotation, DPD), le seed construit l'agrégat DIRECTEMENT et le persiste
 * via le port, SANS passer par le use case : le use case publierait l'événement sur le bus
 * (boucle BPM déclenchée → les démos ne resteraient pas DÉPOSÉES) et ferait échouer le démarrage
 * si RabbitMQ n'est pas encore prêt. {@code viderEvenements()} garantit qu'aucune donnée de démo
 * ne part sur le bus.
 *
 * <p>IDEMPOTENT par référence DÉTERMINISTE ({@code VB-DEMO-{agence}-00n}) : l'événement est
 * rejoué (démarrage + chaque enregistrement de profil), la garde par référence empêche tout
 * doublon, versement par versement. Un échec de semis est loggué et n'interrompt jamais le
 * démarrage (les gardes permettent le rattrapage au rejeu suivant).
 */
@Component
public class VersementsDemoListener {

    private static final Logger log = LoggerFactory.getLogger(VersementsDemoListener.class);

    /** Gabarits de démo : suffixe de référence, mois, montant, date, bordereau, banque, reçu. */
    private record Gabarit(String suffixe, String mois, String montant, LocalDate dateVersement,
                           String bordereau, String banque, String recu) {
    }

    private static final List<Gabarit> GABARITS = List.of(
            new Gabarit("001", "2026-05", "1850000", LocalDate.of(2026, 6, 5), "BRD-2026-05", "BNA", "recu-mai.pdf"),
            new Gabarit("002", "2026-04", "1620000", LocalDate.of(2026, 5, 6), "BRD-2026-04", "BEA", "recu-avril.pdf"));

    private final VersementRepository repository;
    private final Clock horloge;

    public VersementsDemoListener(VersementRepository repository, Clock horloge) {
        this.repository = repository;
        this.horloge = horloge;
    }

    @EventListener
    public void surAgencesDeclarees(AgencesDeclareesEvent evenement) {
        for (String agence : evenement.codesAgences()) {
            try {
                semerPourAgence(agence);
            } catch (RuntimeException e) {
                // Une donnée de démo ne doit JAMAIS empêcher le démarrage ni bloquer les autres agences.
                log.warn("Seed de démo des versements impossible pour l'agence {} : {}", agence, e.getMessage());
            }
        }
    }

    private void semerPourAgence(String agence) {
        Instant maintenant = horloge.instant();
        for (Gabarit gabarit : GABARITS) {
            String reference = "VB-DEMO-%s-%s".formatted(agence, gabarit.suffixe());
            if (repository.trouverParReference(reference).isPresent()) {
                continue; // déjà semé : ne pas dupliquer (idempotence par versement)
            }
            Versement versement = Versement.creerBrouillon(
                    agence,
                    MoisSituation.depuis(gabarit.mois()),
                    new BigDecimal(gabarit.montant()),
                    gabarit.dateVersement(),
                    gabarit.bordereau(),
                    gabarit.banque(),
                    "Versement de démo",
                    "Démo",
                    List.of(new PieceJustificative(gabarit.recu(), "GED-DEMO-" + gabarit.recu())),
                    maintenant);
            versement.soumettre(reference, maintenant);
            versement.viderEvenements(); // démo : rien ne part sur le bus (reste DÉPOSÉ)
            repository.enregistrer(versement);
        }
    }
}
