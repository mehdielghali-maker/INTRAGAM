// Stockage local (IndexedDB via Dexie) : déclarations + pièces (blobs ENTIERS), organisés par
// déclaration (idLocal) et par TYPE de pièce. Consultable hors-ligne (dossier interne à l'app).

import Dexie, { Table } from 'dexie';
import { Declaration, Piece, StatutSync, TypePiece } from '@decsin';

/** Déclaration stockée localement, avec son statut de synchronisation. */
export interface DeclarationLocale extends Declaration {
  statutSync: StatutSync;
  erreur?: string;
  majLe: number;
}

/** Pièce stockée localement : métadonnées + blob entier (photo/document). */
export interface PieceLocale {
  id: string;
  idLocalDeclaration: string;
  type: TypePiece;
  nom: string;
  estPdf: boolean;
  tailleKo: number;
  dataUrl?: string;
  blob: Blob;
  reference?: string;
}

class DeclarationsDb extends Dexie {
  declarations!: Table<DeclarationLocale, string>;
  pieces!: Table<PieceLocale, string>;

  constructor() {
    super('gam-declarations');
    this.version(1).stores({
      declarations: 'idLocal, code, statutSync',
      pieces: 'id, idLocalDeclaration, type',
    });
  }
}

export const db = new DeclarationsDb();

/** Enregistre/écrase une déclaration locale (idempotent par idLocal). */
export async function enregistrerDeclaration(decl: Declaration, statutSync: StatutSync): Promise<void> {
  await db.declarations.put({ ...decl, statutSync, majLe: Date.now() });
}

/** Ajoute (ou remplace) une pièce d'un type donné pour une déclaration. */
export async function ajouterPiece(idLocalDeclaration: string, piece: Piece, blob: Blob): Promise<void> {
  // Une seule pièce par type : on retire l'éventuelle précédente.
  await db.pieces.where({ idLocalDeclaration, type: piece.type }).delete();
  await db.pieces.put({
    id: piece.id,
    idLocalDeclaration,
    type: piece.type,
    nom: piece.nom,
    estPdf: piece.estPdf,
    tailleKo: piece.tailleKo,
    dataUrl: piece.dataUrl,
    blob,
  });
}

export async function supprimerPiece(idLocalDeclaration: string, type: TypePiece): Promise<void> {
  await db.pieces.where({ idLocalDeclaration, type }).delete();
}

export async function piecesDe(idLocalDeclaration: string): Promise<PieceLocale[]> {
  return db.pieces.where({ idLocalDeclaration }).toArray();
}

export async function declarationsASynchroniser(): Promise<DeclarationLocale[]> {
  return db.declarations.where('statutSync').anyOf('en_attente', 'erreur').toArray();
}

export async function majStatutSync(idLocal: string, statutSync: StatutSync, erreur?: string): Promise<void> {
  await db.declarations.update(idLocal, { statutSync, erreur, majLe: Date.now() });
}

export async function getDeclarationLocale(idLocal: string): Promise<DeclarationLocale | undefined> {
  return db.declarations.get(idLocal);
}

/** Purge totale (déconnexion : on n'oublie rien d'utilisateur sur l'appareil). */
export async function purgerTout(): Promise<void> {
  await db.declarations.clear();
  await db.pieces.clear();
}
