package dz.gam.poste.versement.adapter.in.messaging;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.versement.domain.port.in.DeposerVersementCommand;
import dz.gam.poste.versement.domain.port.in.SoumettreVersementUseCase;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sème quelques versements bancaires FICTIFS (état DÉPOSÉ) par agence en réaction à
 * {@link AgencesDeclareesEvent} — au démarrage et à l'ajout d'une agence via l'admin — afin
 * que le « Suivi des versements » soit peuplé pour la démo de l'AGA.
 *
 * <p>Idempotent : l'événement est rejoué (démarrage + chaque enregistrement de profil), donc on
 * sème UNIQUEMENT pour les agences qui ne portent encore aucun versement
 * ({@link VersementRepository#existsByCodeAgence(String)}). Tout passe par le use case
 * (rien en dur d'un système de référence, pas de cycle entre modules).
 */
@Component
public class VersementsDemoListener {

    private final SoumettreVersementUseCase soumettre;
    private final VersementRepository versementRepository;

    public VersementsDemoListener(SoumettreVersementUseCase soumettre, VersementRepository versementRepository) {
        this.soumettre = soumettre;
        this.versementRepository = versementRepository;
    }

    @EventListener
    public void surAgencesDeclarees(AgencesDeclareesEvent evenement) {
        for (String agence : evenement.codesAgences()) {
            if (versementRepository.existsByCodeAgence(agence)) {
                continue; // déjà semé pour cette agence : on ne duplique pas
            }
            soumettre.soumettre(agence, "Démo", new DeposerVersementCommand(
                    "2026-05",
                    new BigDecimal("1850000"),
                    LocalDate.of(2026, 6, 5),
                    "BRD-2026-05",
                    "BNA",
                    "Versement de démo",
                    List.of("recu-mai.pdf")));
            soumettre.soumettre(agence, "Démo", new DeposerVersementCommand(
                    "2026-04",
                    new BigDecimal("1620000"),
                    LocalDate.of(2026, 5, 6),
                    "BRD-2026-04",
                    "BEA",
                    "Versement de démo",
                    List.of("recu-avril.pdf")));
        }
    }
}
