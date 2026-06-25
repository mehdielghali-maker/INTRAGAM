package dz.gam.poste.proassurmock;

import dz.gam.poste.cheque.domain.port.in.ChequeEmisCommand;
import dz.gam.poste.cheque.domain.port.in.EnregistrerChequeEmisUseCase;
import dz.gam.poste.contexte.config.ContexteProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sème quelques chèques FICTIFS par agence au démarrage (simule des émissions PROASSUR), pour
 * que le « Suivi des chèques » montre des dossiers PAR AGENCE. Idempotent : l'enregistrement
 * d'un chèque est idempotent par référence. À remplacer par les vraies émissions PROASSUR.
 */
@Component
@Order(100) // après les contextes Spring ; l'enregistrement est synchrone (pas via le bus)
public class ChequesDemoSeeder implements ApplicationRunner {

    private static final List<String> BENEFICIAIRES =
            List.of("SARL Baticonstruct", "EURL Trans-Med", "SPA Yassir", "Mme Bensalem", "SARL Délices");

    private final EnregistrerChequeEmisUseCase enregistrer;
    private final ContexteProperties contexte;

    public ChequesDemoSeeder(EnregistrerChequeEmisUseCase enregistrer, ContexteProperties contexte) {
        this.enregistrer = enregistrer;
        this.contexte = contexte;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> agences = new LinkedHashSet<>();
        contexte.profils().forEach(p -> p.agences().forEach(a -> agences.add(a.code())));

        for (String agence : agences) {
            for (int n = 1; n <= 3; n++) {
                String reference = "CHQ-%s-%03d".formatted(agence, n);
                enregistrer.enregistrer(new ChequeEmisCommand(
                        reference,
                        BigDecimal.valueOf(50_000L * n + 25_000L),
                        "DZD",
                        BENEFICIAIRES.get((Math.abs(reference.hashCode())) % BENEFICIAIRES.size()),
                        agence,
                        LocalDate.of(2026, 6, 10 + n)));
            }
        }
    }
}
