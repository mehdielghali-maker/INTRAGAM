import {
  DeposerVersementRequest,
  SituationMois,
  StatutVersement,
  VersementOptions,
  VersementResponse,
} from './types';

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    let detail = `Erreur HTTP ${reponse.status}`;
    try {
      const c = await reponse.json();
      detail = c.detail || c.message || detail;
    } catch {
      /* corps non-JSON */
    }
    throw new Error(detail);
  }
  return reponse.json() as Promise<T>;
}

/** Options du formulaire : banques + mois sélectionnables. */
export async function getOptions(): Promise<VersementOptions> {
  return lireJson(await fetch('/api/versement/options', { credentials: 'same-origin' }));
}

/** Situation financière du mois (émis / encaissé / versé / reste), agence active côté back. */
export async function getSituation(mois: string): Promise<SituationMois> {
  return lireJson(
    await fetch(`/api/versement/situation?mois=${encodeURIComponent(mois)}`, {
      credentials: 'same-origin',
    }),
  );
}

/** Versements déposés (filtre optionnel par statut). */
export async function listerVersements(statut?: StatutVersement | ''): Promise<VersementResponse[]> {
  return lireJson(
    await fetch(`/api/versement${statut ? `?statut=${statut}` : ''}`, { credentials: 'same-origin' }),
  );
}

/** Enregistre un brouillon. */
export async function enregistrerBrouillon(req: DeposerVersementRequest): Promise<VersementResponse> {
  return lireJson(
    await fetch('/api/versement/brouillon', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    }),
  );
}

/** Soumet directement le versement au BPM. */
export async function soumettre(req: DeposerVersementRequest): Promise<VersementResponse> {
  return lireJson(
    await fetch('/api/versement/soumettre', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    }),
  );
}

/** Soumet au BPM un brouillon existant. */
export async function soumettreBrouillon(id: string): Promise<VersementResponse> {
  return lireJson(
    await fetch(`/api/versement/${encodeURIComponent(id)}/soumettre`, {
      method: 'POST',
      credentials: 'same-origin',
    }),
  );
}

export const FILTRES_VERSEMENT: { cle: StatutVersement | ''; libelle: string }[] = [
  { cle: '', libelle: 'Tous' },
  { cle: 'DEPOSE', libelle: 'Déposé' },
  { cle: 'EN_CONTROLE', libelle: 'En contrôle' },
  { cle: 'VALIDE', libelle: 'Validé' },
  { cle: 'REJETE', libelle: 'Rejeté' },
];
