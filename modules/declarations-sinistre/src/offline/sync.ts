// File de synchronisation (flux 2 temps) : vide les déclarations « en attente » dans le BON
// ORDRE — d'abord chaque pièce (PostAldFile → référence), puis la déclaration (SaveDeclarationAld
// avec ces références). Idempotent (idLocal). À déclencher au retour réseau ET à chaque ouverture.

import { decsin, ReferenceFichier } from '@decsin';
import { declarationsASynchroniser, majStatutSync, piecesDe } from './db';

export interface ResultatSync {
  traitees: number;
  synchronisees: number;
  echecs: number;
}

let enCours = false;

/** Synchronise toutes les déclarations en attente ou en erreur. */
export async function synchroniser(): Promise<ResultatSync> {
  if (enCours || !navigator.onLine) {
    return { traitees: 0, synchronisees: 0, echecs: 0 };
  }
  enCours = true;
  const resultat: ResultatSync = { traitees: 0, synchronisees: 0, echecs: 0 };
  try {
    const aFaire = await declarationsASynchroniser();
    for (const decl of aFaire) {
      resultat.traitees++;
      try {
        // 1) chaque pièce → référence
        const pieces = await piecesDe(decl.idLocal);
        const references: ReferenceFichier[] = [];
        for (const p of pieces) {
          const ref = await decsin.postFile(p.blob, p.type, decl.code);
          references.push(ref);
        }
        // 2) la déclaration avec les références
        await decsin.saveDeclaration(decl, references);
        await majStatutSync(decl.idLocal, 'synchronise'); // on conserve les blobs en local
        resultat.synchronisees++;
      } catch (e) {
        await majStatutSync(decl.idLocal, 'erreur', e instanceof Error ? e.message : 'Échec');
        resultat.echecs++;
      }
    }
  } finally {
    enCours = false;
  }
  return resultat;
}

/** Branche la synchro automatique au retour du réseau. */
export function activerSyncAuto(onChange?: () => void): () => void {
  const handler = () => {
    void synchroniser().then(() => onChange?.());
  };
  window.addEventListener('online', handler);
  return () => window.removeEventListener('online', handler);
}
