// Session locale de l'agent terrain : ouverte en ligne (login agent + OTP), persistée pour l'usage
// hors-ligne, expirée après VITE_SESSION_AGENT_JOURS. Déconnexion = purge totale du local.

import { config, SessionAgent } from '@souscription';
import { purgerTout } from './offline/db';

const CLE = 'souscription.session';

export interface SessionLocale {
  codeAgent: string;
  nomAgent: string;
  expireLe: number;
}

export function ouvrirSession(s: SessionAgent): SessionLocale {
  const session: SessionLocale = {
    codeAgent: s.codeAgent,
    nomAgent: s.nomAgent,
    expireLe: Date.now() + config.sessionAgentJours * 24 * 60 * 60 * 1000,
  };
  localStorage.setItem(CLE, JSON.stringify(session));
  return session;
}

export function sessionCourante(): SessionLocale | null {
  try {
    const brut = localStorage.getItem(CLE);
    if (!brut) {
      return null;
    }
    const s = JSON.parse(brut) as SessionLocale;
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
