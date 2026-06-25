// Administration des profils SSO (AGA/agents) et de leurs agences.

export type ProfilUtilisateur = 'AGA' | 'AGENT';

export interface AgenceProfil {
  code: string;
  nom: string;
}

export interface Profil {
  identifiant: string;
  nomAffiche: string;
  profil: ProfilUtilisateur;
  agences: AgenceProfil[];
}

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    const detail = await reponse.text();
    throw new Error(detail || `Erreur HTTP ${reponse.status}`);
  }
  return reponse.json() as Promise<T>;
}

async function verifier(reponse: Response): Promise<void> {
  if (!reponse.ok) {
    const detail = await reponse.text();
    throw new Error(detail || `Erreur HTTP ${reponse.status}`);
  }
}

const J: RequestInit = { credentials: 'same-origin' };
const H = { 'Content-Type': 'application/json' };

export async function getProfils(): Promise<Profil[]> {
  return lireJson(await fetch('/api/admin/profils', J));
}

export async function creerProfil(p: Profil): Promise<Profil> {
  return lireJson(await fetch('/api/admin/profils', { ...J, method: 'POST', headers: H, body: JSON.stringify(p) }));
}

export async function modifierProfil(p: Profil): Promise<Profil> {
  return lireJson(
    await fetch(`/api/admin/profils/${encodeURIComponent(p.identifiant)}`, {
      ...J,
      method: 'PUT',
      headers: H,
      body: JSON.stringify(p),
    }),
  );
}

export async function supprimerProfil(identifiant: string): Promise<void> {
  await verifier(await fetch(`/api/admin/profils/${encodeURIComponent(identifiant)}`, { ...J, method: 'DELETE' }));
}

/** Profil actif de la session (mock SSO). */
export async function getProfilActif(): Promise<string> {
  const etat = await lireJson<{ profilActif: string }>(await fetch('/api/mock/identite', J));
  return etat.profilActif;
}

/** Active un profil (simule la connexion de cet utilisateur) pour la session. */
export async function activerProfil(identifiant: string): Promise<void> {
  await verifier(
    await fetch('/api/mock/identite/actif', { ...J, method: 'POST', headers: H, body: JSON.stringify({ identifiant }) }),
  );
}
