// Client REST typé du suivi des chèques. Reflète contracts/openapi/poste-api.yaml.

export type StatutCheque =
  | 'EMIS'
  | 'IMPRIME'
  | 'REMIS_AGENCE'
  | 'REMIS_BENEFICIAIRE'
  | 'ENCAISSE'
  | 'RETOURNE';

export interface DossierCheque {
  id: string;
  reference: string;
  montant: number;
  devise: string;
  beneficiaire: string;
  agence: string;
  dateEmission: string;
  statut: StatutCheque;
  prochainsStatuts: StatutCheque[];
  dateCreation: string;
  dateDerniereMaj: string;
}

export interface FiltreCheques {
  statut?: StatutCheque | '';
  agence?: string;
}

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    const detail = await reponse.text();
    throw new Error(detail || `Erreur HTTP ${reponse.status}`);
  }
  return reponse.json() as Promise<T>;
}

export async function listerCheques(filtre: FiltreCheques = {}): Promise<DossierCheque[]> {
  const params = new URLSearchParams();
  if (filtre.statut) params.set('statut', filtre.statut);
  if (filtre.agence) params.set('agence', filtre.agence);
  const query = params.toString();
  return lireJson(await fetch(`/api/cheques${query ? `?${query}` : ''}`));
}

export async function obtenirCheque(id: string): Promise<DossierCheque> {
  return lireJson(await fetch(`/api/cheques/${id}`));
}

export async function faireAvancerStatut(
  id: string,
  statutCible: StatutCheque,
): Promise<DossierCheque> {
  return lireJson(
    await fetch(`/api/cheques/${id}/statut`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ statutCible }),
    }),
  );
}

export const LIBELLE_STATUT: Record<StatutCheque, string> = {
  EMIS: 'Émis',
  IMPRIME: 'Imprimé',
  REMIS_AGENCE: "Remis à l'agence",
  REMIS_BENEFICIAIRE: 'Remis au bénéficiaire',
  ENCAISSE: 'Encaissé',
  RETOURNE: 'Retourné',
};
