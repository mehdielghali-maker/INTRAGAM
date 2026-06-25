package dz.gam.poste.proassurmock;

import dz.gam.poste.cheque.domain.port.in.ChequeEmisCommand;
import dz.gam.poste.cheque.domain.port.in.EnregistrerChequeEmisUseCase;
import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sème quelques chèques FICTIFS par agence (simule des émissions PROASSUR) en réaction à
 * {@link AgencesDeclareesEvent} — au démarrage et à l'ajout d'une agence via l'admin — afin
 * que le « Suivi des chèques » soit peuplé par agence. Idempotent (par référence).
 */
@Component
public class ChequesDemoListener {

    private static final List<String> BENEFICIAIRES =
            List.of("SARL Baticonstruct", "EURL Trans-Med", "SPA Yassir", "Mme Bensalem", "SARL Délices");

    private final EnregistrerChequeEmisUseCase enregistrer;

    public ChequesDemoListener(EnregistrerChequeEmisUseCase enregistrer) {
        this.enregistrer = enregistrer;
    }

    @EventListener
    public void surAgencesDeclarees(AgencesDeclareesEvent evenement) {
        for (String agence : evenement.codesAgences()) {
            for (int n = 1; n <= 3; n++) {
                String reference = "CHQ-%s-%03d".formatted(agence, n);
                enregistrer.enregistrer(new ChequeEmisCommand(
                        reference,
                        BigDecimal.valueOf(50_000L * n + 25_000L),
                        "DZD",
                        BENEFICIAIRES.get(Math.abs(reference.hashCode()) % BENEFICIAIRES.size()),
                        agence,
                        LocalDate.of(2026, 6, 10 + n)));
            }
        }
    }
}
