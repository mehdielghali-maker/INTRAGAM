package dz.gam.poste.attestation.domain.port.out;

import dz.gam.poste.attestation.domain.model.ReleveProduction;

import java.util.List;
import java.util.Optional;

/**
 * Persistance des relevés de production VALIDÉS (état du poste uniquement, ADR 0004 :
 * le transactionnel reste dans PROASSUR/Sage). Un relevé par (agence, mois) — la recherche
 * par ce couple porte la garde « déjà validé » du service.
 */
public interface ReleveProductionRepository {

    ReleveProduction enregistrer(ReleveProduction releve);

    Optional<ReleveProduction> trouverParAgenceEtMois(String codeAgence, String mois);

    /** Relevés validés des agences données (périmètre actif), du plus récent au plus ancien. */
    List<ReleveProduction> listerPourAgences(List<String> codesAgences);
}
