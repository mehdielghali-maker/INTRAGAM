import { ContexteCotation, CreerDemandePayload, DemandeCotation, StatutCotation } from './types';

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    let detail = `Erreur HTTP ${reponse.status}`;
    try {
      const corps = await reponse.json();
      detail = corps.detail || corps.message || detail;
    } catch {
      /* corps non-JSON */
    }
    throw new Error(detail);
  }
  return reponse.json() as Promise<T>;
}

export async function listerDemandes(statut?: StatutCotation | ''): Promise<DemandeCotation[]> {
  const query = statut ? `?statut=${statut}` : '';
  return lireJson(await fetch(`/api/cotation${query}`));
}

export async function consulterContexte(): Promise<ContexteCotation> {
  return lireJson(await fetch('/api/cotation/contexte'));
}

export async function envoyerDemande(payload: CreerDemandePayload): Promise<DemandeCotation> {
  return lireJson(
    await fetch('/api/cotation/envoyer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  );
}

export async function enregistrerBrouillon(payload: CreerDemandePayload): Promise<DemandeCotation> {
  return lireJson(
    await fetch('/api/cotation/brouillon', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  );
}

export async function marquerSansSuite(id: string, motif: string): Promise<DemandeCotation> {
  return lireJson(
    await fetch(`/api/cotation/${id}/sans-suite`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ motif }),
    }),
  );
}

/**
 * Déclencheur de DÉMO uniquement : simule l'impression de la quittance côté PROASSUR
 * (en production, cet événement viendrait de l'ERP, pas d'une action de l'UI).
 */
export async function simulerQuittance(reference: string): Promise<void> {
  const r = await fetch(`/api/mock/cotation/${encodeURIComponent(reference)}/quittance`, {
    method: 'POST',
  });
  if (!r.ok) throw new Error(`Erreur HTTP ${r.status}`);
}

/** Filtres de la barre d'outils (clé statut → libellé court). « Toutes » = pas de filtre. */
export const FILTRES_STATUT: { cle: StatutCotation | ''; libelle: string }[] = [
  { cle: '', libelle: 'Toutes' },
  { cle: 'ENVOYEE', libelle: 'Envoyée' },
  { cle: 'EN_COURS', libelle: 'En cours' },
  { cle: 'A_FINALISER', libelle: 'À finaliser' },
  { cle: 'AFFAIRE_GAGNEE', libelle: 'Affaire gagnée' },
];
