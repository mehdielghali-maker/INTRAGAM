package dz.gam.poste.reconnaissance.domain.model;

/**
 * Résultat métier d'une vérification de véhicule : le statut de conformité
 * (plaque ↔ contrat) enrichi de ce que la reconnaissance a lu. {@code bloquant}
 * indique si la validation AGA doit être refusée (anti-fraude configurable via
 * reco.bloque-non-conforme).
 */
public record ResultatVerificationVehicule(
        StatutVerification statut,
        String plaqueLue,        // plaque normalisée lue par le service, ou null
        String typeVehicule,     // "voiture", "camion", ... ou null
        double confiance,        // confiance de la lecture de plaque [0..1]
        boolean estVehicule,
        boolean bloquant) {      // true => la validation AGA est refusée
}
