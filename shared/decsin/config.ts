// Configuration de la fonction déclaration — lue depuis l'environnement Vite (rien en dur).
// Toutes les valeurs ont un défaut « mock/dev » ; le réel se branche via .env (cf. .env.example).

interface EnvBrut {
  VITE_DECSIN_MODE?: string;
  VITE_DECSIN_BASE_URL?: string;
  VITE_DECSIN_API_KEY?: string;
  VITE_DECSIN_API_KEY_HEADER?: string;
  VITE_SESSION_TERRAIN_JOURS?: string;
  VITE_CODE_PREFIXE?: string;
}

// import.meta.env est fourni par Vite (apps et Vitest). Repli {} hors contexte Vite.
const env: EnvBrut = (typeof import.meta !== 'undefined' && (import.meta as { env?: EnvBrut }).env) || {};

export interface DecsinConfig {
  mode: 'mock' | 'real';
  baseUrl: string;
  apiKey: string;
  apiKeyHeader: string;
  sessionTerrainJours: number;
  codePrefixe: string;
}

export const config: DecsinConfig = {
  mode: env.VITE_DECSIN_MODE === 'real' ? 'real' : 'mock',
  baseUrl: env.VITE_DECSIN_BASE_URL || 'https://api2.gam.dz/DECSIN/api/',
  apiKey: env.VITE_DECSIN_API_KEY || '',
  // [DSI à confirmer] nom de l'en-tête de clé d'API.
  apiKeyHeader: env.VITE_DECSIN_API_KEY_HEADER || 'X-Api-Key',
  // [à confirmer] durée de session terrain (défaut 7 jours).
  sessionTerrainJours: Number(env.VITE_SESSION_TERRAIN_JOURS) || 7,
  // [à confirmer] format du code : DEC-XXXX-AAAA.
  codePrefixe: env.VITE_CODE_PREFIXE || 'DEC',
};

/** Libellés métier des statuts (centralisés ; configurables au besoin). */
export const LIBELLES_STATUT: Record<string, string> = {
  LIEN_ENVOYE: 'Lien envoyé',
  A_VALIDER: 'À valider',
  VALIDEE: 'Validée',
  INCOMPLETE: 'Incomplète',
};
