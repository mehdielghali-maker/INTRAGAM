// Stockage local (IndexedDB via Dexie) : souscriptions + pièces (blobs entiers) par souscription
// et par type. Consultable hors-ligne (dossier interne à l'app).

import Dexie, { Table } from 'dexie';
import { Piece, Souscription, StatutSync, TypePieceSouscription } from '@souscription';

export interface SouscriptionLocale extends Souscription {
  statutSync: StatutSync;
  erreur?: string;
  majLe: number;
}

export interface PieceLocale {
  id: string;
  idLocalSouscription: string;
  type: TypePieceSouscription;
  nom: string;
  estPdf: boolean;
  tailleKo: number;
  dataUrl?: string;
  blob: Blob;
  reference?: string;
}

class SouscriptionDb extends Dexie {
  souscriptions!: Table<SouscriptionLocale, string>;
  pieces!: Table<PieceLocale, string>;

  constructor() {
    super('gam-souscription');
    this.version(1).stores({
      souscriptions: 'idLocal, reference, statutSync',
      pieces: 'id, idLocalSouscription, type',
    });
  }
}

export const db = new SouscriptionDb();

export async function enregistrerSouscriptionLocale(s: Souscription, statutSync: StatutSync): Promise<void> {
  await db.souscriptions.put({ ...s, statutSync, majLe: Date.now() });
}

export async function ajouterPiece(idLocalSouscription: string, piece: Piece, blob: Blob): Promise<void> {
  await db.pieces.where({ idLocalSouscription, type: piece.type }).delete();
  await db.pieces.put({
    id: piece.id,
    idLocalSouscription,
    type: piece.type,
    nom: piece.nom,
    estPdf: piece.estPdf,
    tailleKo: piece.tailleKo,
    dataUrl: piece.dataUrl,
    blob,
  });
}

export async function supprimerPiece(idLocalSouscription: string, type: TypePieceSouscription): Promise<void> {
  await db.pieces.where({ idLocalSouscription, type }).delete();
}

export async function piecesDe(idLocalSouscription: string): Promise<PieceLocale[]> {
  return db.pieces.where({ idLocalSouscription }).toArray();
}

export async function souscriptionsASynchroniser(): Promise<SouscriptionLocale[]> {
  return db.souscriptions.where('statutSync').anyOf('en_attente', 'erreur').toArray();
}

export async function majStatutSync(idLocal: string, statutSync: StatutSync, erreur?: string): Promise<void> {
  await db.souscriptions.update(idLocal, { statutSync, erreur, majLe: Date.now() });
}

export async function getSouscriptionLocale(idLocal: string): Promise<SouscriptionLocale | undefined> {
  return db.souscriptions.get(idLocal);
}

export async function purgerTout(): Promise<void> {
  await db.souscriptions.clear();
  await db.pieces.clear();
}
