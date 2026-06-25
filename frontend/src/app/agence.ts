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

/** Nœud du périmètre : une agence et ses sous-agences (vide = agence autonome). */
export interface GroupeAgence {
  code: string;
  nom: string;
  sousAgences: AgenceRef[];
}

export interface ContexteAgence {
  utilisateur: UtilisateurContexte;
  /** Unité d'action sélectionnée ; null si la sélection couvre plusieurs agences (lecture seule). */
  agenceActive: AgenceRef | null;
  /** Code sélectionné : sous-agence, agence parente (consolidé du groupe) ou « CONSOLIDE ». */
  selectionCode: string;
  selectionLibelle: string;
  /** Périmètre hiérarchique (agences + sous-agences) pour le commutateur. */
  perimetre: GroupeAgence[];
  consolideDisponible: boolean;
  /** Sélection en lecture seule (groupe parent ou consolidé global) : aucune action. */
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
