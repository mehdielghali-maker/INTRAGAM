// Point d'entrée du domaine SOUSCRIPTION AUTO : sélectionne l'implémentation (mock|réel) et
// ré-exporte contrats, catalogue de capture et utilitaires.

import { config } from './config';
import { souscriptionHttp } from './souscriptionHttp';
import { souscriptionMock } from './souscriptionMock';
import { SouscriptionPort } from './souscriptionPort';

/** Adaptateur actif : mock en dev, réel si VITE_SOUSCRIPTION_MODE=real. */
export const souscription: SouscriptionPort = config.mode === 'real' ? souscriptionHttp : souscriptionMock;

export * from './types';
export { config, LIBELLES_STATUT } from './config';
export { catalogueSouscription, peutEnvoyer, piecesManquantes, REQUIS } from './piecesSouscription';
export { CODE_OTP_AGENT, genererReference, souscriptionsDemo } from './souscriptionMock';
export { brouillonsLocaux } from './brouillonsLocaux';
export type { SouscriptionPort, CriteresRecherche } from './souscriptionPort';
