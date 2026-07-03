// Files de synchronisation des attestations (pattern modules/declarations-sinistre/src/offline/sync.ts) :
// 1) FILE OCR — idempotente par id de ligne : hors-ligne ou échec, la ligne reste 'a_lire' et la
//    photo reste en file ; on retente au chargement et au retour du réseau ('online').
// 2) FILE DE VALIDATION — un relevé validé hors-ligne passe 'A_VALIDER' localement, puis est
//    POSTé à la reconnexion → 'VALIDEE' (référence + date). HTTP 409 = déjà validé côté serveur
//    pour (agence, mois) → on verrouille aussi localement.

import { ErreurHttp, validerReleve } from './api';
import {
  entreeFileOcr,
  entreesFileOcr,
  lignesDuLot,
  majLigne,
  majReleve,
  obtenirLigne,
  relevesAValider,
  retirerDeFileOcr,
} from './db';
import { contratVersChamps, estComptabilisee, lignePourEnvoi, reduireStatutLigne } from './logiqueReleve';
import { lireAttestation } from './ocrClient';
import { ContratLecture } from './types';

/**
 * Tente la lecture OCR d'UNE ligne en file (utilisée par la capture en direct ET par la file).
 * Idempotente : sans entrée en file (déjà lue) → null sans effet. Échec → la ligne reste 'a_lire'.
 */
export async function lireLigneEnFile(idLigne: string): Promise<ContratLecture | null> {
  const entree = await entreeFileOcr(idLigne);
  if (!entree) return null;
  const ligne = await obtenirLigne(idLigne);
  if (!ligne) {
    await retirerDeFileOcr(idLigne); // ligne supprimée entre-temps : on purge la file
    return null;
  }
  const contrat = await lireAttestation(entree.blob);
  if (!contrat) return null; // hors-ligne / échec → reste en file ('a_lire')
  await majLigne(idLigne, {
    ...contratVersChamps(contrat),
    confiance: contrat.confiance,
    statutLigne: reduireStatutLigne(ligne.statutLigne, 'ocr_reussi'),
  });
  await retirerDeFileOcr(idLigne);
  return contrat;
}

let ocrEnCours = false;

/** Vide la file OCR (au chargement + au retour du réseau). Renvoie le nombre de lectures abouties. */
export async function traiterFileOcr(): Promise<number> {
  if (ocrEnCours || !navigator.onLine) return 0;
  ocrEnCours = true;
  let lues = 0;
  try {
    for (const entree of await entreesFileOcr()) {
      if (!navigator.onLine) break;
      if (await lireLigneEnFile(entree.idLigne)) lues++;
    }
  } finally {
    ocrEnCours = false;
  }
  return lues;
}

let validationEnCours = false;

/** Envoie les relevés 'A_VALIDER' (validés hors-ligne) ; les lignes en lecture ne partent jamais. */
export async function traiterFileValidation(): Promise<number> {
  if (validationEnCours || !navigator.onLine) return 0;
  validationEnCours = true;
  let validees = 0;
  try {
    for (const releve of await relevesAValider()) {
      const lignes = (await lignesDuLot(releve.agence, releve.mois)).filter((l) =>
        estComptabilisee(l.statutLigne),
      );
      try {
        const reponse = await validerReleve(releve.mois, lignes.map(lignePourEnvoi));
        await majReleve(releve.cle, {
          statut: 'VALIDEE',
          reference: reponse.reference,
          dateValidation: new Date().toISOString(),
        });
        validees++;
      } catch (e) {
        if (e instanceof ErreurHttp && e.statut === 409) {
          // Déjà validé côté serveur pour (agence, mois) → on verrouille localement aussi.
          await majReleve(releve.cle, { statut: 'VALIDEE', dateValidation: new Date().toISOString() });
        }
        // Autre erreur : le relevé reste 'A_VALIDER', retentera au prochain passage.
      }
    }
  } finally {
    validationEnCours = false;
  }
  return validees;
}

/** Passe complète (OCR puis validations). */
export async function synchroniserAttestations(): Promise<void> {
  await traiterFileOcr();
  await traiterFileValidation();
}

/**
 * Branche la synchro : une passe immédiate (chargement) + une à chaque retour du réseau.
 * Renvoie la fonction de désabonnement (pattern useEffect).
 */
export function activerSyncAttestations(onChange?: () => void): () => void {
  const handler = () => {
    void synchroniserAttestations().then(() => onChange?.());
  };
  handler();
  window.addEventListener('online', handler);
  return () => window.removeEventListener('online', handler);
}
