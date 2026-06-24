package dz.gam.poste.cotation.config;

import dz.gam.poste.cotation.domain.model.StatutCotation;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Paramètres de la fonction « Demande de cotation » (préfixe {@code poste.cotation}).
 * Libellés de statut, branches et délais du mock central : tout est configurable,
 * jamais figé en dur.
 *
 * @param prefixeReference préfixe du N° de demande (ex. {@code DC})
 * @param referenceSeed    amorce du compteur de référence (cohérence démo)
 * @param branches         options de la liste « Branche / Objet »
 * @param libelles         libellés fr par statut (clé = nom de l'enum)
 * @param mock             délais simulés du central
 * @param identite         identité SSO mockée en dev
 */
@ConfigurationProperties(prefix = "poste.cotation")
public record CotationProperties(
        String prefixeReference,
        int referenceSeed,
        List<String> branches,
        Map<StatutCotation, String> libelles,
        Mock mock,
        Identite identite) {

    public record Mock(long delaiPriseEnChargeMs, long delaiCotationMs) {
    }

    public record Identite(String utilisateur, String codeAgence, String nomAgence, String directionRegionale) {
    }
}
