import { config } from './config';
import { RecoPort } from './recoPort';
import { ResultatReco } from './types';

/**
 * Adapter RÉEL : appelle directement le microservice RECO (FastAPI) en multipart
 * (`POST {baseUrl}/analyser`). Le service doit autoriser le CORS (appel navigateur). Les noms
 * de champs JSON (estVehicule, typeVehicule, plaque, confiance, confianceVehicule) sont mappés
 * explicitement. En cas d'échec (réseau / service indisponible), on LÈVE : la reconnaissance est
 * un confort, l'appelant (bandeau) traite l'erreur sans bloquer la déclaration.
 */
export const recoHttp: RecoPort = {
  async analyser(photo, vue) {
    const form = new FormData();
    form.append('photo', photo, 'photo.jpg');
    if (vue) form.append('vue', vue);

    const url = `${config.baseUrl.replace(/\/$/, '')}/analyser`;
    const reponse = await fetch(url, { method: 'POST', body: form });
    if (!reponse.ok) {
      throw new Error(`RECO HTTP ${reponse.status}`);
    }
    const j = (await reponse.json()) as Partial<ResultatReco>;
    return {
      estVehicule: Boolean(j.estVehicule),
      typeVehicule: j.typeVehicule ?? null,
      plaque: j.plaque ?? null,
      confiance: typeof j.confiance === 'number' ? j.confiance : 0,
      confianceVehicule: typeof j.confianceVehicule === 'number' ? j.confianceVehicule : 0,
    };
  },
};
