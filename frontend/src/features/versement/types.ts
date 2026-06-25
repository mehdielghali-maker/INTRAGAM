// Contrats du module « Versement bancaire » (miroir des records backend).

export type StatutVersement = 'BROUILLON' | 'DEPOSE' | 'EN_CONTROLE' | 'VALIDE' | 'REJETE';

export interface MoisOption {
  valeur: string; // ex. 2026-06
  libelle: string; // ex. Juin 2026 — en cours
}

export interface VersementOptions {
  banques: string[];
  mois: MoisOption[];
}

export interface SituationMois {
  codeAgence: string;
  moisValeur: string;
  moisLibelle: string;
  productionEmise: number;
  encaisse: number;
  dejaVerse: number;
  resteARegulariser: number;
  pourcentage: number;
  aRegulariser: boolean;
}

export interface PieceVersement {
  nomFichier: string;
  gedId: string;
}

export interface VersementResponse {
  id: string;
  reference: string | null;
  codeAgence: string;
  moisSituation: string;
  moisLibelle: string;
  montantVerse: number;
  dateVersement: string;
  referenceBordereau: string | null;
  banque: string | null;
  commentaire: string | null;
  createur: string;
  statut: StatutVersement;
  statutLibelle: string;
  prochainsStatuts: StatutVersement[];
  referenceBpm: string | null;
  motifRejet: string | null;
  dateDepot: string | null;
  dateCreation: string;
  dateMaj: string;
  pieces: PieceVersement[];
}

export interface DeposerVersementRequest {
  moisSituation: string;
  montantVerse: number | null;
  dateVersement: string; // YYYY-MM-DD
  referenceBordereau?: string;
  banque?: string;
  commentaire?: string;
  nomsFichiers: string[];
}
