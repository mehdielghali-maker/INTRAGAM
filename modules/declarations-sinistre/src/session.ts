// Session locale du client terrain : ouverte en ligne (login + double facteur), persistée
// pour permettre l'usage hors-ligne, expirée après VITE_SESSION_TERRAIN_JOURS. La sécurité est
// rejouée à la synchronisation. La déconnexion purge tout le local.

import { config, LoginResponse } from '@decsin';
import { purgerTout } from './offline/db';

const CLE = 'decsin.session';

export interface SessionClient {
  codeClient: string;
  idConducteur: string;
  nomConducteur: string;
  telephone: string;
  expireLe: number; // epoch ms
}

export function ouvrirSession(login: LoginResponse, telephone: string): SessionClient {
  const session: SessionClient = {
    codeClient: login.codeClient,
    idConducteur: login.idConducteur,
    nomConducteur: login.nomConducteur,
    telephone,
    expireLe: Date.now() + config.sessionTerrainJours * 24 * 60 * 60 * 1000,
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

/** Déconnexion : efface la session ET purge le dossier local (RGPD / appareil partagé). */
export async function deconnecter(): Promise<void> {
  localStorage.removeItem(CLE);
  await purgerTout();
}
