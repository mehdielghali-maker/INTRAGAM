// Contrat de lecture du contexte d'agence (miroir des records backend du module `contexte`).

export type ProfilUtilisateur = 'AGA' | 'AGENT';

export interface AgenceRef {
  code: string;
  nom: string;
}

export interface UtilisateurContexte {
  identifiant: string;
  nomAffiche: string;
  profil: ProfilUtilisateur;
}

export interface ContexteAgence {
  utilisateur: UtilisateurContexte;
  /** Agence active ; null en mode consolidé. */
  agenceActive: AgenceRef | null;
  agencesAutorisees: AgenceRef[];
  consolideDisponible: boolean;
  /** Vue consolidée active (« Toutes mes agences ») : lecture seule, aucune action. */
  consolideActif: boolean;
}

/** Sentinelle envoyée au back pour activer la vue consolidée. */
export const CODE_CONSOLIDE = 'CONSOLIDE';

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    throw new Error(`Erreur HTTP ${reponse.status}`);
  }
  return reponse.json() as Promise<T>;
}

/** Lit le contexte d'agence (utilisateur, agence active, périmètre) depuis la session serveur. */
export async function getContexte(): Promise<ContexteAgence> {
  return lireJson(await fetch('/api/contexte', { credentials: 'same-origin' }));
}

/**
 * Change l'agence active. Le back revérifie le périmètre (403 si hors périmètre) et la
 * mémorise en session serveur. Aucun stockage navigateur.
 */
export async function changerAgenceActive(code: string): Promise<ContexteAgence> {
  return lireJson(
    await fetch('/api/contexte/agence-active', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
    }),
  );
}
