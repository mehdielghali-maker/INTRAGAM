export type StatutDpd = 'BROUILLON' | 'ENVOYEE' | 'EN_VALIDATION' | 'ACCORDEE' | 'REFUSEE';
export type StatutReglement = 'REGLEE' | 'ECHUE' | 'A_ECHOIR';
export type TypePersonne = 'MORALE' | 'PHYSIQUE';
export type TypePiece = 'RC' | 'AUTRE';

export interface Souscription {
  noProposition: string;
  dateProposition: string;
  devisCreePar: string;
  montantPrime: number;
  branche: string;
  dateEffet: string;
  dateEcheance: string;
  dureeContratMois: number;
  noPolice: string | null;
}

export interface InfoClient {
  nomAssure: string;
  nomSouscripteur: string;
  telephone: string | null;
  cnrc: string | null;
  typePersonne: TypePersonne;
  institutionPublique: boolean;
  adresse: string | null;
}

export interface PieceRef {
  type: TypePiece;
  nomFichier: string;
  gedId: string;
}

export interface DemandeDpd {
  id: string;
  reference: string | null;
  statut: StatutDpd;
  statutLibelle: string;
  prochainsStatuts: StatutDpd[];
  avenant: boolean;
  commentaire: string;
  souscription: Souscription;
  infoClient: InfoClient;
  codeAgence: string;
  mailAgence: string;
  directionRegionale: string;
  mailDirectionRegionale: string;
  createur: string;
  validateurNom: string | null;
  validateurInitiales: string | null;
  motifRefus: string | null;
  codeAccord: string | null;
  dateDemande: string;
  dateMaj: string;
  pieces: PieceRef[];
}

export interface IdentiteDpd {
  utilisateur: string;
  codeAgence: string;
  mailAgence: string;
  nomAgence: string;
  directionRegionale: string;
  mailDirectionRegionale: string;
}

export interface ContexteDpd {
  identite: IdentiteDpd;
  libellesStatut: Record<string, string>;
  niveauValidation: string;
}

export interface PrefillProposition {
  souscription: Souscription;
  client: InfoClient;
}

export interface Echeance {
  numero: number;
  datePrevue: string;
  montant: number;
  statutReglement: StatutReglement;
  dateReglement: string | null;
}

export interface ResumeAccord {
  noAccord: string;
  assure: string;
  policeOuProposition: string;
  montantPrime: number;
  statut: string;
  dateDerniereMaj: string;
}

export interface AccordProassur {
  resume: ResumeAccord;
  echeances: Echeance[];
}

export interface AccordSuiviResponse {
  codeAccord: string;
  resume: ResumeAccord;
  versionCourante: number;
  nbVersions: number;
  total: number;
  nbReglees: number;
  montantRegle: number;
  nbRestantes: number;
  montantRestant: number;
}

export interface CreerDpdPayload {
  noProposition: string;
  avenant: boolean;
  commentaire?: string;
  nomAssure: string;
  nomSouscripteur: string;
  telephone?: string;
  cnrc?: string;
  typePersonne: TypePersonne;
  institutionPublique: boolean;
  adresse?: string;
  fichiersRc: string[];
  fichiersAutres: string[];
}
