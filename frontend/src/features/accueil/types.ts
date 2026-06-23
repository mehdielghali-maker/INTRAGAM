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

export interface CarteEcart {
  valeur: number;
  pourcentage: number;
  aRegulariser: boolean;
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
}

export type Periode = 'MOIS_COURANT' | 'YTD';
