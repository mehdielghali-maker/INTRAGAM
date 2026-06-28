// File de synchronisation (flux 2 temps) : pour chaque souscription « en attente », envoie chaque
// pièce (postFile → référence) puis enregistre la souscription avec ces références. Déclenchée au
// retour réseau ET à chaque ouverture. Idempotente (idLocal).

import { ReferenceFichier, souscription } from '@souscription';
import { majStatutSync, piecesDe, souscriptionsASynchroniser } from './db';

export interface ResultatSync {
  traitees: number;
  synchronisees: number;
  echecs: number;
}

let enCours = false;

export async function synchroniser(): Promise<ResultatSync> {
  if (enCours || !navigator.onLine) {
    return { traitees: 0, synchronisees: 0, echecs: 0 };
  }
  enCours = true;
  const resultat: ResultatSync = { traitees: 0, synchronisees: 0, echecs: 0 };
  try {
    const aFaire = await souscriptionsASynchroniser();
    for (const s of aFaire) {
      resultat.traitees++;
      try {
        const pieces = await piecesDe(s.idLocal);
        const references: ReferenceFichier[] = [];
        for (const p of pieces) {
          const ref = await souscription.postFile(p.blob, p.type, s.reference);
          references.push(ref);
        }
        await souscription.enregistrerSouscription(s, references);
        await majStatutSync(s.idLocal, 'synchronise');
        resultat.synchronisees++;
      } catch (e) {
        await majStatutSync(s.idLocal, 'erreur', e instanceof Error ? e.message : 'Échec');
        resultat.echecs++;
      }
    }
  } finally {
    enCours = false;
  }
  return resultat;
}

export function activerSyncAuto(onChange?: () => void): () => void {
  const handler = () => {
    void synchroniser().then(() => onChange?.());
  };
  window.addEventListener('online', handler);
  return () => window.removeEventListener('online', handler);
}
