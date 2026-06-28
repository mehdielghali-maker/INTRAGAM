// Catalogue de pièces GÉNÉRIQUE qui pilote le socle de capture (déclaration, souscription, …).
// Chaque domaine fournit un Catalogue ; les composants du socle ne dépendent QUE de ce contrat.

/** Définition d'une pièce/vue (type neutre : string, pas d'union spécifique à un domaine). */
export interface DefinitionPiece {
  type: string;
  libelle: string;
  sousTitre: string;
  groupe: string;
  obligatoire: boolean;
  masque: boolean; // vue véhicule avec silhouette de cadrage
}

export interface GroupeDef {
  id: string;
  libelle: string;
  hint?: string; // texte d'aide affiché à côté du libellé de groupe
}

/** Pièce capturée (forme neutre, structurellement compatible avec les Piece des domaines). */
export interface PieceCapturee {
  id: string;
  type: string;
  nom: string;
  dataUrl?: string;
  estPdf: boolean;
  tailleKo: number;
}

/**
 * Catalogue d'un domaine. `groupeVehicule` désigne le groupe rendu via le sélecteur de vues +
 * silhouettes ; les autres groupes sont rendus en tuiles. `estObligatoire`/`piecesManquantes`
 * portent la règle de validation (incluant les conditions, ex. assurance adverse si tiers).
 */
export interface Catalogue {
  groupes: GroupeDef[];
  definitions: DefinitionPiece[];
  silhouettes: Record<string, string>; // type de vue → SVG
  iconesVue: Record<string, string>; // type de vue → SVG d'icône (chip)
  groupeVehicule?: string;
  // ctx: any volontaire — contexte de validation propre à chaque domaine (ex. { tiers }) ;
  // frontière « plugin » entre le socle générique et les catalogues métier.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  estObligatoire: (type: string, ctx: any) => boolean;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  piecesManquantes: (pieces: { type: string }[], ctx: any) => string[];
}

export function piecesDuGroupe(cat: Catalogue, groupe: string): DefinitionPiece[] {
  return cat.definitions.filter((d) => d.groupe === groupe);
}

export function vuesVehicule(cat: Catalogue): DefinitionPiece[] {
  return cat.groupeVehicule ? piecesDuGroupe(cat, cat.groupeVehicule) : [];
}

export function definition(cat: Catalogue, type: string): DefinitionPiece | undefined {
  return cat.definitions.find((d) => d.type === type);
}
