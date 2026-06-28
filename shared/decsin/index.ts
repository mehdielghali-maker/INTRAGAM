// Point d'entrée de la couche partagée DECSIN. Sélectionne l'implémentation (mock|réel)
// selon la configuration, et ré-exporte les contrats + utilitaires communs aux 2 faces.

import { config } from './config';
import { decsinHttp } from './decsinHttp';
import { decsinMock } from './decsinMock';
import { DecsinPort } from './decsinPort';

/** Adaptateur actif : mock en dev, réel si VITE_DECSIN_MODE=real. */
export const decsin: DecsinPort = config.mode === 'real' ? decsinHttp : decsinMock;

/**
 * Rattache à PROASSUR les déclarations validées HORS-LIGNE (drapeau aRattacher) — à appeler à
 * la reconnexion. La validation (rattachement + N° sinistre) nécessite le réseau ; la capture,
 * elle, fonctionne hors-ligne. Renvoie le nombre de déclarations rattachées.
 */
export async function rattacherDeclarationsEnAttente(): Promise<number> {
  if (typeof navigator !== 'undefined' && !navigator.onLine) {
    return 0;
  }
  const liste = await decsin.getDeclarations();
  let n = 0;
  for (const d of liste) {
    if (d.statut === 'A_VALIDER' && d.aRattacher) {
      const { numSinistre, idDossierSinistre } = await decsin.rattacherDeclaration(d);
      await decsin.creerDeclaration({ ...d, statut: 'VALIDEE', numSinistre, idDossierSinistre, aRattacher: false });
      n++;
    }
  }
  return n;
}

export * from './types';
export * from './pieces';
export * from './media';
export { config, LIBELLES_STATUT } from './config';
export { CODE_OTP_DEMO, genererCode } from './decsinMock';
export { brouillonsLocaux } from './brouillonsLocaux';
export type { DecsinPort } from './decsinPort';
export { catalogueDeclaration } from './catalogue';
export type { ContexteDeclaration } from './catalogue';
