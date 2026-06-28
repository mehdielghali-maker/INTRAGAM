// Adaptateur HTTP réel vers le backend GAM (/SecGam/*), activé par VITE_SOUSCRIPTION_MODE=real.
// Endpoints reconstitués depuis l'APK « UNF Expert GAM » ; casse exacte des champs et nom/valeur
// de la clé d'API restent [DSI à confirmer] — le mapping est isolé ici, ajustable sans toucher l'UI.

import { config } from './config';
import { CriteresRecherche, SouscriptionPort } from './souscriptionPort';
import { ChampsOcr, Entite, FiltreSouscription, PreEntite, SessionAgent, Souscription } from './types';

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
    throw new Error(`GAM ${reponse.status} : ${await reponse.text()}`);
  }
  return reponse.json() as Promise<T>;
}

export const souscriptionHttp: SouscriptionPort = {
  async loginAgent(login, motDePasse, otp) {
    const r = await fetch(url('SecGam/loginAgentGam'), {
      method: 'POST',
      headers: entetes(),
      body: JSON.stringify({ login, motDePasse, otp }),
    });
    return lireJson<SessionAgent>(r);
  },

  async rechercheEntite(criteres: CriteresRecherche) {
    const r = await fetch(url('SecGam/rechercheEntiteGam'), {
      method: 'POST',
      headers: entetes(),
      body: JSON.stringify(criteres),
    });
    return lireJson<Entite[]>(r);
  },

  async listePreEntites(typeProduit) {
    const r = await fetch(url(`SecGam/listePreEntitesGAM?typeProduit=${encodeURIComponent(typeProduit)}`), {
      headers: entetes(false),
    });
    return lireJson<PreEntite[]>(r);
  },

  async getOcrData(imageBase64, typeDoc) {
    // Init OCR puis extraction (initialiserEntitesOcrGAM). [DSI à confirmer].
    const r = await fetch(url('SecGam/initialiserEntitesOcrGAM'), {
      method: 'POST',
      headers: entetes(),
      body: JSON.stringify({ image: imageBase64, type: typeDoc }),
    });
    return lireJson<ChampsOcr>(r);
  },

  async getSouscriptions(filtre?: FiltreSouscription) {
    const r = await fetch(url('SecGam/listeSouscriptions'), { headers: entetes(false) });
    const liste = await lireJson<Souscription[]>(r);
    return filtre?.statut ? liste.filter((s) => s.statut === filtre.statut) : liste;
  },

  async getSouscriptionParReference(reference) {
    const liste = await this.getSouscriptions();
    return liste.find((s) => s.reference === reference) ?? null;
  },

  async creerSouscription(s) {
    return this.enregistrerSouscription(s, []);
  },

  async postFile(blob, typeDoc, reference) {
    // 1) upload binaire (attachFile)
    const form = new FormData();
    form.append('file', blob, `${reference}-${typeDoc}.jpg`);
    form.append('type', typeDoc);
    form.append('reference', reference);
    const up = await fetch(url('file/PostAldFile/'), { method: 'POST', headers: entetes(false), body: form });
    await lireJson<unknown>(up);
    // 2) référencement (enregistrerAttachment)
    const ref = await fetch(url('SecGam/enregistrerAttachment'), {
      method: 'POST',
      headers: entetes(),
      body: JSON.stringify({ type: typeDoc, reference, nomFichier: `${reference}-${typeDoc}.jpg` }),
    });
    const data = await lireJson<{ reference: string }>(ref);
    return { type: typeDoc, reference: data.reference };
  },

  async enregistrerSouscription(souscription, references) {
    // Métadonnées en query string (enregistrerMetaDonnees?metadata=).
    const metadata = encodeURIComponent(JSON.stringify({ souscription, references }));
    const r = await fetch(url(`SecGam/enregistrerMetaDonnees?metadata=${metadata}`), {
      method: 'POST',
      headers: entetes(),
    });
    return lireJson<Souscription>(r);
  },
};
