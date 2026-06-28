// Configuration de la souscription auto — lue depuis l'environnement Vite (rien en dur).
// Défauts « mock/dev » ; le réel (backend GAM /SecGam) se branche via .env.

interface EnvBrut {
  VITE_SOUSCRIPTION_MODE?: string;
  VITE_SOUSCRIPTION_BASE_URL?: string;
  VITE_SOUSCRIPTION_API_KEY?: string;
  VITE_SOUSCRIPTION_API_KEY_HEADER?: string;
  VITE_SESSION_AGENT_JOURS?: string;
  VITE_CODE_PREFIXE?: string;
}

const env: EnvBrut = (typeof import.meta !== 'undefined' && (import.meta as { env?: EnvBrut }).env) || {};

export interface SouscriptionConfig {
  mode: 'mock' | 'real';
  baseUrl: string;
  apiKey: string;
  apiKeyHeader: string;
  sessionAgentJours: number;
  codePrefixe: string;
}

export const config: SouscriptionConfig = {
  mode: env.VITE_SOUSCRIPTION_MODE === 'real' ? 'real' : 'mock',
  // API métier GAM (cf. APK : api2.gam.dz/APIS/api/v2). [DSI à confirmer].
  baseUrl: env.VITE_SOUSCRIPTION_BASE_URL || 'https://api2.gam.dz/APIS/api/v2/',
  apiKey: env.VITE_SOUSCRIPTION_API_KEY || '',
  apiKeyHeader: env.VITE_SOUSCRIPTION_API_KEY_HEADER || 'X-Api-Key',
  sessionAgentJours: Number(env.VITE_SESSION_AGENT_JOURS) || 7,
  codePrefixe: env.VITE_CODE_PREFIXE || 'SCR',
};

export const LIBELLES_STATUT: Record<string, string> = {
  BROUILLON: 'Brouillon',
  LIEN_ENVOYE: 'Lien envoyé',
  A_VALIDER: 'À valider',
  RELANCE: 'Relancé',
  VALIDEE: 'Enregistrée',
};
