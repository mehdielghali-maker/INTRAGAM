// Socle « dossier en cours » — machine à états UNIFIÉE pour la déclaration de sinistre ET la
// souscription. Modèle corrigé : PRÉSENTIEL par défaut (l'AGA saisit en agence, BROUILLON
// enregistrable à tout moment) ; LIEN client = exception (le client remplit à distance).
// Le rattachement PROASSUR/DECSIN n'a lieu QU'À LA VALIDATION. Aucun couplage à un domaine.

export type StatutDossier = 'BROUILLON' | 'LIEN_ENVOYE' | 'A_VALIDER' | 'RELANCE' | 'VALIDEE';

/** Comment le dossier a été ouvert : en agence (présentiel, défaut) ou par lien (à distance). */
export type OrigineDossier = 'AGA' | 'CLIENT';

/** Libellés d'affichage (centralisés ; un module peut les surcharger au besoin). */
export const LIBELLES_DOSSIER: Record<StatutDossier, string> = {
  BROUILLON: 'Brouillon',
  LIEN_ENVOYE: 'Lien envoyé',
  A_VALIDER: 'À valider',
  RELANCE: 'Relancé',
  VALIDEE: 'Validée',
};

/**
 * Transitions autorisées.
 * Présentiel : BROUILLON ⇄ (édite/reprend) → A_VALIDER → VALIDEE (ou validation directe d'un
 * brouillon complet). Distant : LIEN_ENVOYE → A_VALIDER → (relance) RELANCE → A_VALIDER → VALIDEE.
 */
export const TRANSITIONS: Record<StatutDossier, StatutDossier[]> = {
  BROUILLON: ['BROUILLON', 'A_VALIDER', 'LIEN_ENVOYE', 'VALIDEE'],
  LIEN_ENVOYE: ['A_VALIDER'],
  A_VALIDER: ['VALIDEE', 'RELANCE', 'BROUILLON'],
  RELANCE: ['A_VALIDER'],
  VALIDEE: [],
};

export function peutTransitionner(de: StatutDossier, vers: StatutDossier): boolean {
  return TRANSITIONS[de]?.includes(vers) ?? false;
}

/** Modifiable (par l'AGA en présentiel) tant que NON validé : Brouillon et « À valider ». */
export function estModifiable(statut: StatutDossier): boolean {
  return statut !== 'VALIDEE';
}

/** Terminal : rattaché à PROASSUR/DECSIN, verrouillé. */
export function estVerrouille(statut: StatutDossier): boolean {
  return statut === 'VALIDEE';
}

/**
 * La VALIDATION exige la complétude (calculée par le module) ; le BROUILLON, JAMAIS.
 * Validable depuis un brouillon présentiel complet, ou depuis « À valider » (après contrôle/
 * soumission client). On ne valide pas depuis RELANCE (on attend le client) ni LIEN_ENVOYE.
 */
export function peutValider(statut: StatutDossier, complet: boolean): boolean {
  return complet && (statut === 'BROUILLON' || statut === 'A_VALIDER');
}

/** Un dossier non validé n'est JAMAIS rattaché au système de référence (PROASSUR/DECSIN). */
export function estRattachable(statut: StatutDossier): boolean {
  return statut === 'VALIDEE';
}
