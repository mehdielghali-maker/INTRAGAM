export type StatutCotation =
  | 'BROUILLON'
  | 'ENVOYEE'
  | 'EN_COURS'
  | 'A_FINALISER'
  | 'AFFAIRE_GAGNEE'
  | 'SANS_SUITE';

export interface PieceJointe {
  nomFichier: string;
  gedId: string;
}

export interface DemandeCotation {
  id: string;
  reference: string | null;
  objet: string;
  nomProspect: string;
  numeroPolice: string | null;
  renouvellement: boolean;
  commentaire: string;
  codeAgence: string;
  directionRegionale: string;
  createur: string;
  statut: StatutCotation;
  statutLibelle: string;
  prochainsStatuts: StatutCotation[];
  souscripteurNom: string | null;
  souscripteurInitiales: string | null;
  numeroProposition: string | null;
  referenceDevis: string | null;
  motifSansSuite: string | null;
  dateQuittance: string | null;
  dateCreation: string;
  dateMaj: string;
  piecesJointes: PieceJointe[];
}

export interface IdentiteAgence {
  utilisateur: string;
  codeAgence: string;
  nomAgence: string;
  directionRegionale: string;
}

export interface ContexteCotation {
  identite: IdentiteAgence;
  branches: string[];
  libellesStatut: Record<string, string>;
}

export interface CreerDemandePayload {
  objet: string;
  nomProspect: string;
  numeroPolice?: string;
  commentaire?: string;
  nomsFichiers: string[];
}
