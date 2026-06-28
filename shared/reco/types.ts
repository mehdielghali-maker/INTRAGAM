// Modèles de la couche RECO (reconnaissance véhicule + lecture de plaque), partagée par les
// faces front qui appellent le microservice RECO INDÉPENDANT directement (sans backend Java).

/** Résultat brut du service RECO (aucune notion de contrat ici). */
export interface ResultatReco {
  estVehicule: boolean;
  typeVehicule: string | null; // "voiture", "camion", ... ou null
  plaque: string | null; // plaque normalisée, ou null si non lue
  confiance: number; // confiance de la lecture de plaque [0..1]
  confianceVehicule: number; // confiance de la détection véhicule [0..1]
}

/** Statut de la comparaison plaque lue ↔ immatriculation du contrat. */
export type StatutVerification =
  | 'CONFORME'
  | 'NON_CONFORME'
  | 'NON_LUE'
  | 'PAS_UN_VEHICULE'
  | 'VUE_SANS_PLAQUE';

/**
 * Résultat métier (lecture + comparaison) consommé par l'UI.
 * NB : volontairement DUPLIQUÉ avec `@sinistre-ui` (type identique structurellement). On ne fusionne
 * pas pour éviter de coupler `@sinistre-ui` à `@reco` (le poste utilise `@sinistre-ui` sans `@reco`).
 * La compatibilité est vérifiée par le compilateur au point d'injection de l'analyseur (typage structurel).
 */
export interface ResultatVerification {
  statut: StatutVerification;
  plaqueLue: string | null;
  typeVehicule: string | null;
  confiance: number;
  estVehicule: boolean;
  bloquant: boolean; // côté client : toujours false (pas d'anti-fraude bloquant)
}
