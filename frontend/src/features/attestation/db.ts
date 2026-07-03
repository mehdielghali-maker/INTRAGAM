// Stockage local (IndexedDB via Dexie) du relevé d'attestations : relevés par clé de lot,
// lignes par id (index composé [agence+mois]) et FILE OCR (photos en attente de lecture).
// Même pattern que modules/declarations-sinistre/src/offline/db.ts.

import Dexie, { Table } from 'dexie';
import { LigneReleve, ReleveLocal, StatutReleve } from './types';
import { cleLot } from './logiqueReleve';

/** Entrée de la file OCR : la photo (blob compressé) d'une ligne encore 'a_lire'. */
export interface EntreeFileOcr {
  /** Id de la LIGNE — la file est idempotente par ligne (une seule lecture en attente). */
  idLigne: string;
  blob: Blob;
  ajouteLe: number;
}

class AttestationsDb extends Dexie {
  releves!: Table<ReleveLocal, string>;
  lignes!: Table<LigneReleve, string>;
  fileOcr!: Table<EntreeFileOcr, string>;

  constructor() {
    super('gam-attestations');
    this.version(1).stores({
      releves: 'cle',
      lignes: 'id, [agence+mois]',
      fileOcr: 'idLigne',
    });
  }
}

export const db = new AttestationsDb();

/**
 * Ouvre (ou RÉOUVRE) le lot (agence active, mois) : renvoie l'existant s'il y en a un,
 * sinon crée un brouillon. Clé unique 'agence|AAAA-MM' → jamais de doublon de lot.
 */
export async function ouvrirLot(agence: string, mois: string): Promise<ReleveLocal> {
  const cle = cleLot(agence, mois);
  const existant = await db.releves.get(cle);
  if (existant) return existant;
  const nouveau: ReleveLocal = { cle, mois, agence, statut: 'BROUILLON' };
  await db.releves.put(nouveau);
  return nouveau;
}

/** Met à jour le relevé (statut, référence, date de validation). */
export async function majReleve(
  cle: string,
  maj: Partial<Pick<ReleveLocal, 'statut' | 'reference' | 'dateValidation'>>,
): Promise<void> {
  await db.releves.update(cle, maj);
}

/** Relevés en attente d'envoi (validés hors-ligne). */
export async function relevesAValider(): Promise<ReleveLocal[]> {
  return db.releves.filter((r) => r.statut === 'A_VALIDER').toArray();
}

/** Lignes du lot (agence, mois), plus récentes d'abord (comme la maquette : ajout en tête). */
export async function lignesDuLot(agence: string, mois: string): Promise<LigneReleve[]> {
  const lignes = await db.lignes.where('[agence+mois]').equals([agence, mois]).toArray();
  return lignes.sort((a, b) => b.dateAjout - a.dateAjout);
}

export async function ajouterLigne(ligne: LigneReleve): Promise<void> {
  await db.lignes.put(ligne);
}

export async function majLigne(id: string, maj: Partial<LigneReleve>): Promise<void> {
  await db.lignes.update(id, maj);
}

export async function obtenirLigne(id: string): Promise<LigneReleve | undefined> {
  return db.lignes.get(id);
}

/** Supprime une ligne ET son éventuelle entrée en file OCR. */
export async function supprimerLigne(id: string): Promise<void> {
  await db.fileOcr.delete(id);
  await db.lignes.delete(id);
}

/** Met (ou remet — re-scan) une photo en file OCR ; idempotent par id de ligne. */
export async function mettreEnFileOcr(idLigne: string, blob: Blob): Promise<void> {
  await db.fileOcr.put({ idLigne, blob, ajouteLe: Date.now() });
}

export async function retirerDeFileOcr(idLigne: string): Promise<void> {
  await db.fileOcr.delete(idLigne);
}

export async function entreeFileOcr(idLigne: string): Promise<EntreeFileOcr | undefined> {
  return db.fileOcr.get(idLigne);
}

export async function entreesFileOcr(): Promise<EntreeFileOcr[]> {
  return db.fileOcr.orderBy('idLigne').toArray();
}

/** Verrou d'écriture d'un lot : plus aucune modification une fois la validation demandée. */
export function estVerrouille(statut: StatutReleve): boolean {
  return statut !== 'BROUILLON';
}

/**
 * Stockage PERSISTANT (pattern PWA) : demande au navigateur de ne pas évincer les photos en
 * attente. Refus → simple avertissement console (alerte silencieuse), l'app reste utilisable.
 */
export async function demanderPersistance(): Promise<boolean> {
  try {
    if (!navigator.storage?.persist) return false;
    if (await navigator.storage.persisted()) return true;
    const accorde = await navigator.storage.persist();
    if (!accorde) {
      console.warn('Stockage persistant refusé : synchronisez rapidement les attestations en attente.');
    }
    return accorde;
  } catch {
    return false;
  }
}
