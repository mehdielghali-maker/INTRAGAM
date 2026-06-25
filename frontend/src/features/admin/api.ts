// Administration des profils SSO (AGA/agents) et de leurs agences.

export type ProfilUtilisateur = 'AGA' | 'AGENT';

export interface AgenceProfil {
  code: string;
  nom: string;
}

export interface Profil {
  identifiant: string;
  /** Login de connexion (saisi par l'AGA, distinct de l'identifiant technique). */
  login: string;
  nomAffiche: string;
  profil: ProfilUtilisateur;
  agences: AgenceProfil[];
  /** Ids des modules autorisés (cf. MODULES). */
  modules: string[];
  /** Mot de passe en clair — uniquement en écriture (création/changement). Jamais relu. */
  motDePasse?: string;
}

/** Compte d'administration : login fixe + adresse e-mail de récupération. */
export interface CompteAdmin {
  login: string;
  emailRecuperation: string | null;
}

/** Modules dont l'accès est gérable par profil (mêmes ids que la navigation). */
export const MODULES: { id: string; label: string }[] = [
  { id: 'depot', label: 'Dépôt Situation Financière' },
  { id: 'versement', label: 'Versement bancaire' },
  { id: 'attestations', label: 'Attestations' },
  { id: 'cheques', label: 'Suivi des chèques' },
  { id: 'bureau', label: "Envois bureau d'ordre" },
  { id: 'cotation', label: 'Demande de cotation' },
  { id: 'expertise', label: "Demande d'expertise" },
  { id: 'accords', label: "Accords d'échéancier" },
  { id: 'echeanciers', label: 'Suivi des échéanciers' },
  { id: 'contentieux', label: 'Créances & contentieux' },
];

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

/** Profil actuellement « activé » en aperçu admin (impersonation), ou null. */
export async function getProfilActif(): Promise<string | null> {
  const etat = await lireJson<{ profilActif: string | null }>(await fetch('/api/admin/identite', J));
  return etat.profilActif;
}

/** Aperçu admin : « active » un profil AGA (impersonation) pour la session. */
export async function activerProfil(identifiant: string): Promise<void> {
  await verifier(
    await fetch('/api/admin/identite/actif', { ...J, method: 'POST', headers: H, body: JSON.stringify({ identifiant }) }),
  );
}

/** Compte d'administration (login + e-mail de récupération). */
export async function getCompteAdmin(): Promise<CompteAdmin> {
  return lireJson(await fetch('/api/admin/compte', J));
}

/** Change le mot de passe admin (vérifie l'ancien côté back). */
export async function changerMotDePasseAdmin(ancien: string, nouveau: string): Promise<void> {
  await verifier(
    await fetch('/api/admin/compte/mot-de-passe', {
      ...J,
      method: 'PUT',
      headers: H,
      body: JSON.stringify({ ancien, nouveau }),
    }),
  );
}

/** Définit (ou efface) l'adresse e-mail de récupération du compte admin. */
export async function definirEmailRecuperation(email: string): Promise<void> {
  await verifier(
    await fetch('/api/admin/compte/email-recuperation', {
      ...J,
      method: 'PUT',
      headers: H,
      body: JSON.stringify({ email }),
    }),
  );
}
