// Authentification : connexion (admin ou AGA), session, configuration (SSO), récupération.

export type Role = 'ADMIN' | 'AGA';

export interface Principal {
  role: Role;
  login: string;
  nomAffiche: string;
}

export interface ConfigAuth {
  ssoMicrosoftActif: boolean;
}

export interface Recuperation {
  indiceEmail: string | null;
  message: string;
}

/** Compte sélectionnable sur l'écran de connexion (sans mot de passe). */
export interface CompteConnectable {
  login: string;
  libelle: string;
  type: Role;
}

const J: RequestInit = { credentials: 'same-origin' };
const H = { 'Content-Type': 'application/json' };

/** Extrait un message d'erreur lisible (ProblemDetail.detail si présent, sinon texte brut). */
async function messageErreur(reponse: Response): Promise<string> {
  const texte = await reponse.text();
  try {
    const json = JSON.parse(texte);
    return json.detail || json.message || texte || `Erreur HTTP ${reponse.status}`;
  } catch {
    return texte || `Erreur HTTP ${reponse.status}`;
  }
}

/** Utilisateur connecté, ou null si la session n'est pas authentifiée (401). */
export async function getEtat(): Promise<Principal | null> {
  const reponse = await fetch('/api/auth/etat', J);
  if (reponse.status === 401) return null;
  if (!reponse.ok) throw new Error(await messageErreur(reponse));
  return reponse.json() as Promise<Principal>;
}

export async function connexion(login: string, motDePasse: string): Promise<Principal> {
  const reponse = await fetch('/api/auth/login', {
    ...J,
    method: 'POST',
    headers: H,
    body: JSON.stringify({ login, motDePasse }),
  });
  if (!reponse.ok) throw new Error(await messageErreur(reponse));
  return reponse.json() as Promise<Principal>;
}

export async function deconnexion(): Promise<void> {
  await fetch('/api/auth/logout', { ...J, method: 'POST' });
}

export async function getConfig(): Promise<ConfigAuth> {
  const reponse = await fetch('/api/auth/config', J);
  if (!reponse.ok) throw new Error(await messageErreur(reponse));
  return reponse.json() as Promise<ConfigAuth>;
}

/** Comptes sélectionnables sur l'écran de connexion (admin + AGA disposant d'un login). */
export async function getComptes(): Promise<CompteConnectable[]> {
  const reponse = await fetch('/api/auth/comptes', J);
  if (!reponse.ok) throw new Error(await messageErreur(reponse));
  return reponse.json() as Promise<CompteConnectable[]>;
}

export async function motDePasseOublie(): Promise<Recuperation> {
  const reponse = await fetch('/api/auth/mot-de-passe-oublie', { ...J, method: 'POST' });
  if (!reponse.ok) throw new Error(await messageErreur(reponse));
  return reponse.json() as Promise<Recuperation>;
}
