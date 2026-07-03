// API des relevés (FIGÉE) : validation d'un lot et liste des relevés validés du périmètre.
// L'AGENCE vient TOUJOURS du contexte serveur (agencePourAction) — jamais envoyée par le front.

import { LigneEnvoi, ReleveValide, ReponseValidation } from './types';

/** Erreur HTTP porteuse du statut (409 = relevé déjà validé pour (agence, mois)). */
export class ErreurHttp extends Error {
  constructor(
    public readonly statut: number,
    message: string,
  ) {
    super(message);
  }
}

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    let detail = `Erreur HTTP ${reponse.status}`;
    try {
      const corps = await reponse.json();
      detail = corps.detail || corps.message || detail;
    } catch {
      /* corps non-JSON */
    }
    throw new ErreurHttp(reponse.status, detail);
  }
  return reponse.json() as Promise<T>;
}

/** Valide le relevé du mois pour l'agence ACTIVE (contexte serveur). 409 si déjà validé. */
export async function validerReleve(mois: string, lignes: LigneEnvoi[]): Promise<ReponseValidation> {
  return lireJson(
    await fetch('/api/attestations/releves', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ mois, lignes }),
    }),
  );
}

/** Relevés VALIDÉS du périmètre actif (sert aussi à verrouiller un lot déjà validé ailleurs). */
export async function listerRelevesValides(): Promise<ReleveValide[]> {
  return lireJson(await fetch('/api/attestations/releves', { credentials: 'same-origin' }));
}
