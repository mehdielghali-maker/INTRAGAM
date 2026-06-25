package dz.gam.poste.versement.config;

import dz.gam.poste.versement.domain.model.StatutVersement;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Paramètres de la fonction « Versement bancaire » (préfixe {@code poste.versement}).
 * Tout est configurable, jamais figé en dur.
 *
 * @param prefixeReference     préfixe de la référence interne (ex. {@code VB})
 * @param referenceSeed        amorce du compteur de référence
 * @param seuilRegularisation  seuil d'alerte « reste à régulariser » (cohérent avec l'accueil)
 * @param banques              liste des banques proposées
 * @param moisDisponibles      mois sélectionnables (AAAA-MM), le 1er = mois en cours
 * @param mock                 comportement simulé du BPM
 */
@ConfigurationProperties(prefix = "poste.versement")
public record VersementProperties(
        String prefixeReference,
        int referenceSeed,
        SeuilRegularisation seuilRegularisation,
        List<String> banques,
        List<String> moisDisponibles,
        Mock mock) {

    public record SeuilRegularisation(BigDecimal montant, BigDecimal pourcentage) {
    }

    /**
     * @param delaiControleMs délai avant passage « En contrôle »
     * @param delaiDecisionMs délai avant décision finale
     * @param decision        décision simulée du BPM (VALIDE par défaut ; REJETE pour tester)
     */
    public record Mock(long delaiControleMs, long delaiDecisionMs, StatutVersement decision) {
    }
}
