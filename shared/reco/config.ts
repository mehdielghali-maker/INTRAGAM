// Configuration de la couche RECO — lue depuis l'environnement Vite (rien en dur).
// Défaut « mock/dev » ; le réel se branche via .env (VITE_RECO_*). Le service RECO est
// INDÉPENDANT (microservice auto-hébergé) : la PWA l'appelle directement.

interface EnvBrut {
  VITE_API_MODE?: string; // bascule GLOBALE mock|real (repli si VITE_RECO_MODE absent)
  VITE_RECO_MODE?: string;
  VITE_RECO_BASE_URL?: string;
}

// import.meta.env est fourni par Vite (apps et Vitest). Repli {} hors contexte Vite.
const env: EnvBrut = (typeof import.meta !== 'undefined' && (import.meta as { env?: EnvBrut }).env) || {};

export interface RecoConfig {
  mode: 'mock' | 'real';
  baseUrl: string;
}

export const config: RecoConfig = {
  // Priorité au switch par domaine ; repli sur la bascule globale VITE_API_MODE ; défaut mock.
  mode: (env.VITE_RECO_MODE ?? env.VITE_API_MODE) === 'real' ? 'real' : 'mock',
  // URL du microservice RECO (FastAPI). En dev natif : http://localhost:8088.
  baseUrl: env.VITE_RECO_BASE_URL || 'http://localhost:8088',
};
