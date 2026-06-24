import {
  AccordProassur,
  AccordSuiviResponse,
  ContexteDpd,
  CreerDpdPayload,
  DemandeDpd,
  PrefillProposition,
  StatutDpd,
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

export async function listerDpd(statut?: StatutDpd | ''): Promise<DemandeDpd[]> {
  return lireJson(await fetch(`/api/dpd${statut ? `?statut=${statut}` : ''}`));
}

export async function contexteDpd(): Promise<ContexteDpd> {
  return lireJson(await fetch('/api/dpd/contexte'));
}

export async function prefillDpd(proposition: string): Promise<PrefillProposition> {
  return lireJson(await fetch(`/api/dpd/prefill?proposition=${encodeURIComponent(proposition)}`));
}

export async function envoyerDpd(payload: CreerDpdPayload): Promise<DemandeDpd> {
  return lireJson(
    await fetch('/api/dpd/envoyer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  );
}

export async function brouillonDpd(payload: CreerDpdPayload): Promise<DemandeDpd> {
  return lireJson(
    await fetch('/api/dpd/brouillon', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  );
}

export async function chargerAccord(code: string): Promise<AccordProassur> {
  return lireJson(await fetch(`/api/dpd/accords/${encodeURIComponent(code)}`));
}

export async function synchroniserAccord(code: string): Promise<AccordSuiviResponse> {
  return lireJson(
    await fetch(`/api/dpd/accords/${encodeURIComponent(code)}/synchroniser`, { method: 'POST' }),
  );
}

/** Démo seulement : déclenche un refus côté central (mock). */
export async function refuserDpd(reference: string, motif: string): Promise<void> {
  const r = await fetch(
    `/api/mock/dpd/${encodeURIComponent(reference)}/refuser?motif=${encodeURIComponent(motif)}`,
    { method: 'POST' },
  );
  if (!r.ok) throw new Error(`Erreur HTTP ${r.status}`);
}

export const FILTRES_DPD: { cle: StatutDpd | ''; libelle: string }[] = [
  { cle: '', libelle: 'Toutes' },
  { cle: 'ENVOYEE', libelle: 'Envoyée' },
  { cle: 'EN_VALIDATION', libelle: 'En validation' },
  { cle: 'ACCORDEE', libelle: 'Accordée' },
  { cle: 'REFUSEE', libelle: 'Refusée' },
];

/** Format montant en DA, séparateur de milliers (locale fr). */
export function formaterDa(montant: number): string {
  return `${montant.toLocaleString('fr-DZ')} DA`;
}

/** Format date courte jj/mm depuis une date ISO ou un Instant. */
export function dateCourte(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
}

/** Format date jj/mm/aaaa. */
export function dateLongue(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}
