// Définition des 3 groupes de pièces et RÈGLE DE VALIDATION unique (partagée AGA + client).

import { Declaration, GroupePiece, TypePiece } from './types';

export interface DefinitionPiece {
  type: TypePiece;
  libelle: string;
  sousTitre: string;
  groupe: GroupePiece;
  obligatoire: boolean; // obligatoire « de base » (hors règle conditionnelle tiers)
  masque: boolean; // affiche le masque de cadrage caméra (4 faces véhicule)
}

/**
 * Catalogue des pièces, dans l'ordre d'affichage. Obligatoires : recto/verso constat + 4 faces.
 * L'assurance adverse est obligatoire de façon CONDITIONNELLE (si tiers) — cf. {@link aUnTiers}.
 */
export const PIECES: DefinitionPiece[] = [
  { type: 'recto_constat', libelle: 'Recto constat', sousTitre: 'photo / galerie', groupe: 'constat', obligatoire: true, masque: false },
  { type: 'verso_constat', libelle: 'Verso constat', sousTitre: 'photo / galerie', groupe: 'constat', obligatoire: true, masque: false },
  { type: 'scene', libelle: 'Scène du sinistre', sousTitre: 'facultatif', groupe: 'constat', obligatoire: false, masque: false },

  { type: 'face_avant', libelle: 'Avant', sousTitre: 'avec guide', groupe: 'vehicule', obligatoire: true, masque: true },
  { type: 'face_arriere', libelle: 'Arrière', sousTitre: 'avec guide', groupe: 'vehicule', obligatoire: true, masque: true },
  { type: 'cote_gauche', libelle: 'Côté gauche', sousTitre: 'avec guide', groupe: 'vehicule', obligatoire: true, masque: true },
  { type: 'cote_droit', libelle: 'Côté droit', sousTitre: 'avec guide', groupe: 'vehicule', obligatoire: true, masque: true },
  // Le toit est une 5e vue PROPOSÉE (avec silhouette) mais FACULTATIVE (non bloquante).
  { type: 'face_toit', libelle: 'Toit', sousTitre: 'facultatif', groupe: 'vehicule', obligatoire: false, masque: true },

  { type: 'permis', libelle: 'Permis de conduire', sousTitre: 'recto / verso', groupe: 'documents', obligatoire: false, masque: false },
  { type: 'carte_grise', libelle: 'Carte grise', sousTitre: 'immatriculation', groupe: 'documents', obligatoire: false, masque: false },
  { type: 'identite', libelle: "Pièce d'identité", sousTitre: 'CNI / passeport', groupe: 'documents', obligatoire: false, masque: false },
  { type: 'assurance_adverse', libelle: 'Assurance adverse', sousTitre: 'si tiers', groupe: 'documents', obligatoire: false, masque: false },
  { type: 'autre', libelle: 'Autres documents', sousTitre: 'facultatif', groupe: 'documents', obligatoire: false, masque: false },
];

export const GROUPES: { id: GroupePiece; libelle: string }[] = [
  { id: 'constat', libelle: 'Constat' },
  { id: 'vehicule', libelle: 'Véhicule' },
  { id: 'documents', libelle: 'Documents' },
];

export function definitionPiece(type: TypePiece): DefinitionPiece | undefined {
  return PIECES.find((p) => p.type === type);
}

export function piecesDuGroupe(groupe: GroupePiece): DefinitionPiece[] {
  return PIECES.filter((p) => p.groupe === groupe);
}

/** Les 5 vues du véhicule (avant, arrière, gauche, droite, toit) dans l'ordre d'affichage. */
export function vuesVehicule(): DefinitionPiece[] {
  return piecesDuGroupe('vehicule');
}

/** Un tiers est déclaré dès qu'une info adverse est renseignée (compagnie OU véhicule). */
export function aUnTiers(d: Pick<Declaration, 'compagnieAdverse' | 'vehiculeAdverse'>): boolean {
  return Boolean(d.compagnieAdverse?.trim() || d.vehiculeAdverse?.trim());
}

/** Types de pièces obligatoires pour CETTE déclaration (intègre la règle conditionnelle tiers). */
export function piecesObligatoires(d: Pick<Declaration, 'compagnieAdverse' | 'vehiculeAdverse'>): TypePiece[] {
  const base = PIECES.filter((p) => p.obligatoire).map((p) => p.type);
  return aUnTiers(d) ? [...base, 'assurance_adverse'] : base;
}

/**
 * Règle de validation UNIQUE : l'envoi est bloqué tant qu'une pièce obligatoire manque.
 * Renvoie la liste des types manquants (vide = envoi autorisé).
 */
export function piecesManquantes(d: Pick<Declaration, 'pieces' | 'compagnieAdverse' | 'vehiculeAdverse'>): TypePiece[] {
  const presents = new Set(d.pieces.map((p) => p.type));
  return piecesObligatoires(d).filter((t) => !presents.has(t));
}

export function peutEnvoyer(d: Pick<Declaration, 'pieces' | 'compagnieAdverse' | 'vehiculeAdverse'>): boolean {
  return piecesManquantes(d).length === 0;
}
