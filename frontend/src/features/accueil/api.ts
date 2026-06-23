import { CompteursNavigation, Periode, TableauBord } from './types';

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    throw new Error(`Erreur HTTP ${reponse.status}`);
  }
  return reponse.json() as Promise<T>;
}

export async function getTableauBord(periode?: Periode): Promise<TableauBord> {
  const query = periode ? `?periode=${periode}` : '';
  return lireJson(await fetch(`/api/agence/tableau-bord${query}`));
}

export async function getNavigation(): Promise<CompteursNavigation> {
  return lireJson(await fetch('/api/agence/navigation'));
}
