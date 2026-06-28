package dz.gam.poste.reconnaissance.domain.model;

/**
 * Résultat de la comparaison « plaque lue ↔ immatriculation du contrat ».
 * Calculé par {@link dz.gam.poste.reconnaissance.domain.service.VerificationPlaqueService}
 * (dans le poste, qui connaît le contrat — pas dans le service RECO).
 */
public enum StatutVerification {
    CONFORME,        // plaque lue == immatriculation du contrat
    NON_CONFORME,    // plaque lue != contrat   -> AVERTISSEMENT « vérifier le véhicule »
    NON_LUE,         // vue avant/arrière mais plaque illisible -> on n'affirme rien
    PAS_UN_VEHICULE, // la photo ne contient pas de véhicule -> nudge qualité
    VUE_SANS_PLAQUE  // gauche/droite/toit : pas de plaque attendue
}
