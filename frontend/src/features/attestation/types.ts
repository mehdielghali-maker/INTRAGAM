// Attestations — relevé mensuel de production (collecte par OCR assisté).
// Types de la feature : miroir du contrat JSON /lire-attestation (FIGÉ) + modèle local (Dexie).

/** Contrat JSON renvoyé par POST /api/attestations/lire (FIGÉ — ne pas modifier). */
export interface ContratLecture {
  /** 15 chiffres attendus. */
  numeroPolice: string | null;
  /** 8 chiffres attendus. */
  numeroQuittance: string | null;
  immatriculation: string | null;
  assure: string | null;
  /** JJ/MM/AAAA. */
  valideDu: string | null;
  valideAu: string | null;
  /** Ex. « 2400,97 ». */
  primeTTC: string | null;
  /** Format NN.AA.NNNN. */
  codeAgence: string | null;
  /** Confiance globale de la lecture, entre 0 et 1. */
  confiance: number;
  statut: 'lu' | 'a_verifier';
  texteBrut: string;
}

/**
 * Statut d'une ligne du relevé local :
 * - `a_lire`     : photo prise, OCR pas encore passé (hors-ligne ou échec) — en file d'attente ;
 * - `lu`         : OCR passé, en attente de confirmation par l'AGA (panneau assisté) ;
 * - `confirme`   : ajoutée au relevé, lecture confirmée ;
 * - `a_verifier` : ajoutée au relevé mais douteuse (garde-fou avant PROASSUR).
 */
export type StatutLigne = 'a_lire' | 'lu' | 'confirme' | 'a_verifier';

/** Champs métier d'une ligne (édition dans le panneau assisté). */
export interface ChampsLigne {
  numeroPolice: string;
  numeroQuittance: string;
  immatriculation: string;
  assure: string;
  valideDu: string;
  valideAu: string;
  primeTTC: string;
  codeAgence: string;
}

/** Ligne du relevé (une attestation photographiée), persistée en local (Dexie). */
export interface LigneReleve extends ChampsLigne {
  /** Identifiant local stable (généré à la capture, idempotence de la file OCR). */
  id: string;
  /** Mois du relevé (AAAA-MM). */
  mois: string;
  /** Code de l'agence ACTIVE au moment de la capture (clé de lot, jamais envoyée pour l'action). */
  agence: string;
  /** Confiance OCR [0..1] (absente tant que non lue). */
  confiance?: number;
  statutLigne: StatutLigne;
  /** Vignette compressée de la photo (réaffichée à l'édition / re-scan). */
  photoDataUrl?: string;
  dateAjout: number;
}

/** Statut du lot local : brouillon ouvert → validation demandée (hors-ligne) → validé (verrouillé). */
export type StatutReleve = 'BROUILLON' | 'A_VALIDER' | 'VALIDEE';

/** Relevé local : UN lot par (agence active, mois). */
export interface ReleveLocal {
  /** Clé 'agence|AAAA-MM' — garantit l'unicité du lot. */
  cle: string;
  mois: string;
  agence: string;
  statut: StatutReleve;
  /** Référence renvoyée par le back à la validation. */
  reference?: string;
  dateValidation?: string;
}

/** Ligne envoyée à POST /api/attestations/releves (API FIGÉE). */
export interface LigneEnvoi extends ChampsLigne {
  statutLigne: 'confirme' | 'a_verifier';
}

/** Réponse de POST /api/attestations/releves. */
export interface ReponseValidation {
  reference: string;
  mois: string;
  codeAgence: string;
  nombreLignes: number;
  message: string;
}

/** Élément de GET /api/attestations/releves (relevés VALIDÉS du périmètre actif). */
export interface ReleveValide {
  reference: string;
  mois: string;
  codeAgence: string;
  nombreLignes: number;
  dateValidation: string;
}
