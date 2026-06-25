// Contrat de lecture du tableau de bord (miroir des records du backend).

export type UniteVariation = 'POURCENT' | 'POINTS';

export interface Variation {
  valeur: number;
  unite: UniteVariation;
  favorable: boolean;
}

export interface CarteMontant {
  valeur: number;
  variation: Variation;
  reference: number;
  libelleReference: string;
}

export type NiveauEcart = 'CORRECT' | 'MODERE' | 'CRITIQUE' | 'DANGER';

export interface CarteEcart {
  valeur: number;
  /** écart / CA annuel extrapolé, en %. */
  pourcentage: number;
  niveau: NiveauEcart;
}

export interface CarteRatio {
  valeur: number;
  variation: Variation;
  reference: number;
}

export interface CompteurAction {
  cle: string;
  valeur: number;
  urgent: boolean;
}

export interface ProductionDepots {
  production: number;
  encaisse: number;
  depose: number;
  ecart: number;
}

/** Ligne de répartition par agence (vue consolidée uniquement). */
export interface RepartitionAgence {
  code: string;
  nom: string;
  caYtd: number;
  encaisse: number;
  depose: number;
  ecart: number;
}

export interface TableauBord {
  agence: { nom: string; code: string };
  periodeLibelle: string;
  dateLibelle: string;
  contratsActifs: number;
  contratsActifsVariation: number;
  caYtd: CarteMontant;
  caMois: CarteMontant;
  ecartDepot: CarteEcart;
  creances: CarteMontant;
  sp: CarteRatio;
  coupDoeil: CompteurAction[];
  productionDepots: ProductionDepots;
  consolide: boolean;
  repartition: RepartitionAgence[];
}

/** Badges de navigation (miroir de CompteursAgence côté backend). */
export interface CompteursNavigation {
  chequesEnAttente: number;
  attestations: number;
  cotations: number;
  echeanciersRisque: number;
  contentieux: number;
  bureauOrdre: number;
  expertises: number;
  accordsEcheancier: number;
  versementsEnCours: number;
}

export type Periode = 'MOIS_COURANT' | 'YTD';
