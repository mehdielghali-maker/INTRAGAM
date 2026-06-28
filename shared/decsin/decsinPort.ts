// Port (interface) de l'accès au backend de déclaration DECSIN + à PROASSUR.
// Une implémentation MOCK (dev) et une implémentation HTTP réelle (prod) le respectent.
// Le choix se fait par configuration (VITE_DECSIN_MODE) — cf. index.ts.

import { Declaration, FiltreDeclaration, LoginResponse, ReferenceFichier, Vehicule } from './types';

export interface DecsinPort {
  // --- PROASSUR (système de référence du sinistre) ---
  /** Pré-remplit le véhicule (marque, n° police, conducteur) depuis l'immatriculation. */
  prefillVehiculeByImmat(immatriculation: string): Promise<Vehicule | null>;
  /** Rattache une déclaration validée → PROASSUR ouvre le dossier et renvoie le N° de sinistre. */
  rattacherDeclaration(declaration: Declaration): Promise<{ numSinistre: string; idDossierSinistre: string }>;

  // --- DECSIN (déclarations) ---
  /** Face client : double facteur (téléphone + code SMS) → session conducteur + véhicules. */
  loginConducteur(telephone: string, code: string): Promise<LoginResponse>;
  /** Liste des déclarations (face AGA : suivi). */
  getDeclarations(filtre?: FiltreDeclaration): Promise<Declaration[]>;
  /** Récupère une déclaration par son code (face client : ouverture du lien). */
  getDeclarationParCode(code: string): Promise<Declaration | null>;
  /** Crée/initialise une déclaration (AGA : saisie ou envoi de lien). */
  creerDeclaration(declaration: Declaration): Promise<Declaration>;
  /** Flux 2 temps — étape 1 : envoie une pièce (multipart) → référence. */
  postFile(blob: Blob, type: ReferenceFichier['type'], codeDeclaration: string): Promise<ReferenceFichier>;
  /** Flux 2 temps — étape 2 : enregistre la déclaration avec les références de pièces. */
  saveDeclaration(declaration: Declaration, references: ReferenceFichier[]): Promise<Declaration>;
}
