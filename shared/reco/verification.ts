import { ResultatReco, ResultatVerification, StatutVerification } from './types';

/** Plaques algériennes = chiffres. On garde l'alphanumérique en majuscules (comme le service). */
function normaliser(s: string | null | undefined): string | null {
  return s == null ? null : s.toUpperCase().replace(/[^A-Z0-9]/g, '');
}

/**
 * Comparaison plaque LUE ↔ immatriculation du CONTRAT — portage TS fidèle du service Java
 * `VerificationPlaqueService` (poste). La plaque n'est attendue que sur les vues avant/arrière ;
 * une lecture incertaine ne doit pas accuser à tort.
 */
export function verifierPlaque(
  reco: ResultatReco | null,
  immatContrat: string | null | undefined,
  vue?: string | null,
): StatutVerification {
  if (!reco) return 'NON_LUE';
  if (!reco.estVehicule) return 'PAS_UN_VEHICULE';
  const vueAvecPlaque = vue == null || vue === 'avant' || vue === 'arriere';
  if (!vueAvecPlaque) return 'VUE_SANS_PLAQUE';
  if (!reco.plaque || reco.plaque.trim() === '') return 'NON_LUE';
  return normaliser(reco.plaque) === normaliser(immatContrat) ? 'CONFORME' : 'NON_CONFORME';
}

/** Assemble le résultat métier (lecture brute + statut de comparaison) pour l'UI. */
export function construireResultat(
  reco: ResultatReco | null,
  immatContrat: string | null | undefined,
  vue?: string | null,
): ResultatVerification {
  return {
    statut: verifierPlaque(reco, immatContrat, vue),
    plaqueLue: reco?.plaque ?? null,
    typeVehicule: reco?.typeVehicule ?? null,
    confiance: reco?.confiance ?? 0,
    estVehicule: !!reco?.estVehicule,
    bloquant: false,
  };
}
