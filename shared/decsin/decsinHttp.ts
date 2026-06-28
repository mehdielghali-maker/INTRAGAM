// Adaptateur HTTP réel vers DECSIN (api2.gam.dz/DECSIN). Activé par VITE_DECSIN_MODE=real.
// La casse exacte des champs et le nom/valeur de la clé d'API restent [DSI à confirmer] :
// le mapping ci-dessous est isolé ici, à ajuster sans toucher au reste de l'app.

import { config } from './config';
import { DecsinPort } from './decsinPort';
import { Declaration, FiltreDeclaration, LoginResponse, Vehicule } from './types';

function url(chemin: string): string {
  return new URL(chemin, config.baseUrl).toString();
}

function entetes(json = true): Record<string, string> {
  const h: Record<string, string> = {};
  if (config.apiKey) {
    h[config.apiKeyHeader] = config.apiKey;
  }
  if (json) {
    h['Content-Type'] = 'application/json';
  }
  return h;
}

async function lireJson<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) {
    throw new Error(`DECSIN ${reponse.status} : ${await reponse.text()}`);
  }
  return reponse.json() as Promise<T>;
}

/**
 * Implémentation réelle. Les endpoints suivent la base documentée ; le mapping
 * Declaration ↔ payload DECSIN est volontairement localisé (étendu pour les 4 faces et les
 * types de documents portés par les pièces). [DSI à confirmer].
 */
export const decsinHttp: DecsinPort = {
  async prefillVehiculeByImmat(immatriculation) {
    // PROASSUR via DECSIN [DSI à confirmer] — endpoint provisoire.
    const reponse = await fetch(url(`v2/SecGAM/VehiculeParImmat?immat=${encodeURIComponent(immatriculation)}`), {
      headers: entetes(false),
    });
    if (reponse.status === 404) {
      return null;
    }
    return lireJson<Vehicule>(reponse);
  },

  async rattacherDeclaration(declaration) {
    const reponse = await fetch(url('v2/SecGAM/RattacherDeclaration'), {
      method: 'POST',
      headers: entetes(),
      body: JSON.stringify({ code: declaration.code, immatriculation: declaration.immatriculation }),
    });
    return lireJson(reponse);
  },

  async loginConducteur(telephone, code) {
    const reponse = await fetch(url('v2/SecGAM/LoginConducteurALD'), {
      method: 'POST',
      headers: entetes(),
      body: JSON.stringify({ telephone, code }),
    });
    return lireJson<LoginResponse>(reponse);
  },

  async getDeclarations(filtre?: FiltreDeclaration) {
    const reponse = await fetch(url('v2/SecGAM/GetDeclarationsSinistresAld'), { headers: entetes(false) });
    const liste = await lireJson<Declaration[]>(reponse);
    return filtre?.statut ? liste.filter((d) => d.statut === filtre.statut) : liste;
  },

  async getDeclarationParCode(code) {
    const reponse = await fetch(url(`v2/SecGAM/GetDeclarationsSinistresAld?code=${encodeURIComponent(code)}`), {
      headers: entetes(false),
    });
    const liste = await lireJson<Declaration[]>(reponse);
    return liste.find((d) => d.code === code) ?? liste[0] ?? null;
  },

  async creerDeclaration(declaration) {
    return this.saveDeclaration(declaration, []);
  },

  async postFile(blob, type, codeDeclaration) {
    const form = new FormData();
    form.append('file', blob, `${codeDeclaration}-${type}.jpg`);
    form.append('type', type);
    form.append('code', codeDeclaration);
    const reponse = await fetch(url('file/PostAldFile/'), { method: 'POST', headers: entetes(false), body: form });
    const ref = await lireJson<{ reference: string }>(reponse);
    return { type, reference: ref.reference };
  },

  async saveDeclaration(declaration, references) {
    const reponse = await fetch(url('v2/SecGAM/SaveDeclarationAld'), {
      method: 'POST',
      headers: entetes(),
      body: JSON.stringify({ declaration, references }),
    });
    return lireJson<Declaration>(reponse);
  },
};
