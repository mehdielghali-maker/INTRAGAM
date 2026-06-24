package dz.gam.poste.dpd.config;

import dz.gam.poste.dpd.domain.model.StatutDpd;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Paramètres de la fonction « Accords d'échéancier » (préfixe {@code poste.dpd}).
 * Libellés de statut, niveau de validation et délais du mock : configurables, jamais en dur.
 */
@ConfigurationProperties(prefix = "poste.dpd")
public record DpdProperties(
        String prefixeReference,
        int referenceSeed,
        Map<StatutDpd, String> libelles,
        String niveauValidation,
        Mock mock,
        Identite identite) {

    public record Mock(long delaiValidationMs, long delaiAccordMs) {
    }

    public record Identite(
            String utilisateur,
            String codeAgence,
            String mailAgence,
            String nomAgence,
            String directionRegionale,
            String mailDirectionRegionale) {
    }
}
