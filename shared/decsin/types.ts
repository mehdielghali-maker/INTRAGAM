// Contrats partagés de la fonction « Déclaration de sinistre » (faces AGA + client).
// Système de référence du sinistre : PROASSUR. Backend de déclaration : DECSIN (externe).

import type { StatutDossier } from '../dossier/etats';

/** Type documentaire d'une pièce — conservé pour le rattachement PROASSUR et l'instruction. */
export type TypePiece =
  | 'recto_constat'
  | 'verso_constat'
  | 'scene'
  | 'face_avant'
  | 'face_arriere'
  | 'cote_gauche'
  | 'cote_droit'
  | 'face_toit'
  | 'permis'
  | 'carte_grise'
  | 'identite'
  | 'assurance_adverse'
  | 'autre';

/** Regroupement d'affichage des pièces. */
export type GroupePiece = 'constat' | 'vehicule' | 'documents';

/** Origine de la déclaration : saisie par l'AGA, ou remplie par le client via le lien. */
export type Origine = 'AGA' | 'CLIENT';

/**
 * Machine à états métier UNIFIÉE (socle @dossier) : présentiel par défaut.
 * BROUILLON (AGA en agence, enregistrable même incomplet) · LIEN_ENVOYE (exception : client à
 * distance) · A_VALIDER (prêt pour contrôle AGA) · RELANCE (renvoyé au client) · VALIDEE (rattachée
 * PROASSUR, verrouillée). Rattachement PROASSUR UNIQUEMENT à la validation.
 */
export type StatutDeclaration = StatutDossier;

/** Statut de synchronisation d'une déclaration locale (face client hors-ligne).
 * brouillon = en cours de saisie (hors file) ; en_attente = à envoyer ; synchronise ; erreur. */
export type StatutSync = 'brouillon' | 'en_attente' | 'synchronise' | 'erreur';

/** Véhicule au contrat (renvoyé par PROASSUR / le login conducteur). */
export interface Vehicule {
  id: string;
  immatriculation: string;
  marque: string;
  numPolice: string;
  conducteur?: string;
  type?: string;
  rang?: number;
}

/**
 * Pièce jointe (photo ou document). Le blob est porté à part (Dexie côté client) ; ici on garde
 * les métadonnées + un aperçu éventuel (dataUrl) et la référence DECSIN après upload.
 */
export interface Piece {
  id: string;
  type: TypePiece;
  nom: string;
  dataUrl?: string; // aperçu compressé (JPEG) ; absent pour un PDF
  estPdf: boolean;
  tailleKo: number;
  reference?: string; // référence renvoyée par PostAldFile (flux 2 temps)
}

/**
 * Déclaration de sinistre. Champs DECSIN observés + extensions (4 faces / types de documents
 * portées par {@link Piece}). {@code idLocal} assure l'idempotence de la synchro.
 */
export interface Declaration {
  idLocal: string; // identifiant local unique (anti-doublon à la synchro)
  code: string; // ex. DEC-XXXX-AAAA
  origine: Origine;
  statut: StatutDeclaration;

  // Contrat / véhicule (pré-rempli depuis PROASSUR via l'immatriculation)
  immatriculation: string;
  marque: string;
  numPolice: string;
  conducteur?: string;

  // Détails du sinistre
  dateSinistre: string; // AAAA-MM-JJ
  heureSinistre: string; // HH:mm
  lieuSinistre: string;
  observations: string; // circonstances
  blesses: boolean;
  telAssure?: string;
  compagnieAdverse?: string;
  vehiculeAdverse?: string;

  // Pièces (3 groupes)
  pieces: Piece[];

  // Côté AGA / suivi
  client?: { nom: string; telephone: string; email?: string };
  observationsAgence?: string;
  dateSaisie?: string;
  numSinistre?: string; // renvoyé par PROASSUR à la validation
  idDossierSinistre?: string;
  /** Validée hors-ligne en attente de rattachement PROASSUR (auto à la reconnexion). */
  aRattacher?: boolean;
}

/** Réponse du login conducteur (face client, double facteur). */
export interface LoginResponse {
  code: number;
  message: string;
  codeClient: string;
  idConducteur: string;
  nomConducteur: string;
  vehicules: Vehicule[];
}

/** Filtre du suivi (face AGA). */
export interface FiltreDeclaration {
  statut?: StatutDeclaration;
}

/** Référence d'un fichier uploadé (flux 2 temps). */
export interface ReferenceFichier {
  type: TypePiece;
  reference: string;
}
