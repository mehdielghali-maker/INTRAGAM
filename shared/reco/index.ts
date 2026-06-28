// Couche RECO partagée (alias @reco) : reconnaissance véhicule + lecture de plaque via le
// microservice INDÉPENDANT. `reco` est l'adapter actif (mock dev / http réel selon VITE_RECO_MODE) ;
// `verifierPlaque`/`construireResultat` font la comparaison plaque ↔ contrat côté client.

import { config } from './config';
import { recoMock } from './recoMock';
import { recoHttp } from './recoHttp';

export const reco = config.mode === 'real' ? recoHttp : recoMock;

export { verifierPlaque, construireResultat } from './verification';
export { config as recoConfig } from './config';
export type { RecoPort } from './recoPort';
export type { ResultatReco, StatutVerification, ResultatVerification } from './types';
