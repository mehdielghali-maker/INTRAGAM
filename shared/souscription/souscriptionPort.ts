// Port (interface) de la souscription auto : auth agent, recherche police, OCR (seam), envoi
// 2 temps (pièce→référence) puis enregistrement. Implémentations MOCK (dev) et HTTP réelle (GAM).

import {
  ChampsOcr,
  Entite,
  FiltreSouscription,
  PreEntite,
  ReferenceFichier,
  SessionAgent,
  Souscription,
  TypeProduit,
} from './types';

export interface CriteresRecherche {
  codeBranche?: string;
  codeSousBranche?: string;
  numeroPolice?: string;
  nomClient?: string;
}

export interface SouscriptionPort {
  /** Authentification agent (login/mot de passe + OTP). */
  loginAgent(login: string, motDePasse: string, otp?: string): Promise<SessionAgent>;
  /** Recherche d'entité police/contrat (rechercheEntiteGam). */
  rechercheEntite(criteres: CriteresRecherche): Promise<Entite[]>;
  /** Pré-entités candidates pour un type de produit (listePreEntitesGAM). */
  listePreEntites(typeProduit: TypeProduit): Promise<PreEntite[]>;
  /** OCR d'une pièce → champs pré-remplis (SEAM — non implémenté : mock figé). */
  getOcrData(imageBase64: string, typeDoc: string): Promise<ChampsOcr>;
  /** Suivi : liste des souscriptions. */
  getSouscriptions(filtre?: FiltreSouscription): Promise<Souscription[]>;
  getSouscriptionParReference(reference: string): Promise<Souscription | null>;
  /** Crée/initialise une souscription (brouillon). */
  creerSouscription(souscription: Souscription): Promise<Souscription>;
  /** Flux 2 temps — étape 1 : envoie une pièce (attachFile + enregistrerAttachment) → référence. */
  postFile(blob: Blob, typeDoc: ReferenceFichier['type'], reference: string): Promise<ReferenceFichier>;
  /** Flux 2 temps — étape 2 : enregistre les métadonnées avec les références (enregistrerMetaDonnees). */
  enregistrerSouscription(souscription: Souscription, references: ReferenceFichier[]): Promise<Souscription>;
}
