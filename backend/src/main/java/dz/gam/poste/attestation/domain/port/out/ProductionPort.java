package dz.gam.poste.attestation.domain.port.out;

import dz.gam.poste.attestation.domain.model.ReleveProduction;

/**
 * Port « production » — c'est le SEAM PROASSUR/Sage : la soumission du relevé validé au
 * système de référence. Aujourd'hui un mock (« Production validée ») ; demain un adapter
 * réel (bus, API PROASSUR ou export Sage) SANS toucher le domaine (ADR 0002). Le poste
 * collecte et transmet ; il ne devient jamais source de vérité transactionnelle.
 */
public interface ProductionPort {

    /**
     * Soumet le relevé validé au système de production.
     *
     * @return l'accusé du système (référence reprise + message d'acquittement)
     */
    RetourProduction soumettre(ReleveProduction releve);

    /** Accusé de soumission renvoyé par le système de production. */
    record RetourProduction(String reference, String message) {
    }

    // ------------------------------------------------------------------------------------
    // SEAM PRÉVU, NON BRANCHÉ (arbitrage validé : collecte seule pour cette tranche) :
    // vérification PROASSUR police par police AVANT l'ajout d'une ligne au relevé.
    // Quand la DSI ouvrira l'accès, décommenter et brancher un adapter dédié — le front
    // affichera alors « police connue / inconnue » à la volée, sans changer le contrat.
    //
    // /** Vérifie l'existence de la police dans PROASSUR (et renvoie l'immatriculation au contrat). */
    // VerificationPolice verifierPolice(String numeroPolice);
    //
    // /** Résultat de la vérification PROASSUR d'un numéro de police. */
    // record VerificationPolice(boolean connue, String immatriculationContrat) {
    // }
    // ------------------------------------------------------------------------------------
}
