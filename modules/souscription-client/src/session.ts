// Session locale du client (face distante de la souscription) : ouverte en ligne (lien + double
// facteur téléphone + code), persistée pour l'usage hors-ligne, expirée après la durée configurée.
// La déconnexion purge tout le local (RGPD / appareil partagé).

import { config } from '@souscription';
import { purgerTout } from './offline/db';

const CLE = 'souscription.client.session';

export interface SessionClient {
  nomClient: string;
  telephone: string;
  reference: string;
  expireLe: number; // epoch ms
}

export function ouvrirSession(nomClient: string, telephone: string, reference: string): SessionClient {
  const session: SessionClient = {
    nomClient,
    telephone,
    reference,
    expireLe: Date.now() + config.sessionAgentJours * 24 * 60 * 60 * 1000,
  };
  localStorage.setItem(CLE, JSON.stringify(session));
  return session;
}

export function sessionCourante(): SessionClient | null {
  try {
    const brut = localStorage.getItem(CLE);
    if (!brut) {
      return null;
    }
    const s = JSON.parse(brut) as SessionClient;
    if (s.expireLe < Date.now()) {
      localStorage.removeItem(CLE);
      return null;
    }
    return s;
  } catch {
    return null;
  }
}

export async function deconnecter(): Promise<void> {
  localStorage.removeItem(CLE);
  await purgerTout();
}
