// Contrats partagés de la fonction « Souscription auto » (section poste + PWA agent).
// Inspiré du principe de l'app « UNF Expert GAM » (recherche police → produit → capture+OCR →
// enregistrement métadonnées + pièces). Backend GAM (/SecGam/*) derrière un adaptateur mock↔réel.

import type { OrigineDossier, StatutDossier } from '../dossier/etats';

/** Produit souscrit — AUTO pour l'instant (architecture extensible). */
export type TypeProduit = 'AUTO';

/** Types de pièces/vues d'une souscription auto (clés neutres, type string côté socle). */
export type TypePieceSouscription =
  | 'cni_recto'
  | 'cni_verso'
  | 'permis_recto'
  | 'permis_verso'
  | 'carte_grise'
  | 'attestation'
  | 'contrat_signe'
  | 'autre'
  | 'veh_avant'
  | 'veh_arriere'
  | 'veh_gauche'
  | 'veh_droit'
  | 'veh_av_g'
  | 'veh_av_d'
  | 'veh_ar_g'
  | 'veh_ar_d'
  | 'veh_vin'
  | 'veh_interieur';

/**
 * Machine à états métier UNIFIÉE (socle @dossier) : présentiel par défaut.
 * BROUILLON · LIEN_ENVOYE (exception distant) · A_VALIDER · RELANCE · VALIDEE (enregistrée GAM, verrouillée).
 */
export type StatutSouscription = StatutDossier;
export type StatutSync = 'brouillon' | 'en_attente' | 'synchronise' | 'erreur';

/** Pièce capturée (même forme que les autres domaines / le socle). */
export interface Piece {
  id: string;
  type: TypePieceSouscription;
  nom: string;
  dataUrl?: string;
  estPdf: boolean;
  tailleKo: number;
  reference?: string; // référence renvoyée par attachFile/enregistrerAttachment
}

/** Résultat de la recherche police/entité (rechercheEntiteGam). */
export interface Entite {
  numeroPolice: string;
  nomClient: string;
  codeBranche: string;
  libelleBranche: string;
  codeSousBranche?: string;
  libelleSousBranche?: string;
  marque?: string;
  immatriculation?: string;
}

/** Pré-entité candidate (listePreEntitesGAM). */
export interface PreEntite {
  id: string;
  libelle: string;
  typeProduit: TypeProduit;
}

/** Session agent (loginAgentGam + OTP). */
export interface SessionAgent {
  code: number; // 0 = OK
  message: string;
  codeAgent: string;
  nomAgent: string;
}

/** Réponse du login CLIENT (face distante : lien + double facteur téléphone + code). */
export interface LoginClientResponse {
  code: number; // 0 = OK
  message: string;
  nomClient?: string;
}

/** Champs pré-remplis par l'OCR (seam — non implémenté, mock figé). */
export interface ChampsOcr {
  prenom?: string;
  nom?: string;
  numero?: string;
  sexe?: string;
  adresse?: string;
}

/** Assuré (saisi / pré-rempli OCR). */
export interface Assure {
  prenom?: string;
  nom?: string;
  numeroCni?: string;
  sexe?: string;
  adresse?: string;
  telephone?: string;
}

/** Véhicule (saisi / pré-rempli OCR carte grise). */
export interface VehiculeContrat {
  immatriculation?: string;
  marque?: string;
  modele?: string;
  energie?: string;
  numeroChassis?: string;
}

/** Une souscription auto. {@code idLocal} assure l'idempotence de la synchro. */
export interface Souscription {
  idLocal: string;
  reference: string; // ex. SCR-XXXX-AAAA
  typeProduit: TypeProduit;
  codeBranche: string;
  libelleBranche?: string;
  codeSousBranche?: string;
  libelleSousBranche?: string;
  numeroPolice: string;
  nomClient: string;
  assure: Assure;
  vehicule: VehiculeContrat;
  pieces: Piece[];
  statut: StatutSouscription;
  /** Présentiel (AGA en agence, défaut) ou client à distance (lien). */
  origine?: OrigineDossier;
  dateSaisie?: string;
  agence?: { code: string; nom: string };
  /** Côté client (face distante) : contact pour le lien. */
  client?: { nom: string; telephone: string; email?: string };
}

/** Référence d'un fichier uploadé (flux 2 temps). */
export interface ReferenceFichier {
  type: TypePieceSouscription;
  reference: string;
}

/** Filtre du suivi. */
export interface FiltreSouscription {
  statut?: StatutSouscription;
}
