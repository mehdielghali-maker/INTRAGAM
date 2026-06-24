import { IconName } from './icons';
import { CompteursNavigation } from '../features/accueil/types';

/**
 * Configuration des 9 fonctionnalités métier de la navigation, en 2 lots.
 * « Suivi des chèques » et « Accueil » sont implémentés ; les autres pointent vers une
 * page placeholder (« à venir »).
 */
export interface NavItem {
  id: string;
  label: string;
  route: string;
  icon: IconName;
  /** clé du compteur de badge dans CompteursNavigation (numérique) */
  badgeKey?: keyof CompteursNavigation;
  /** badge textuel (ex. « À faire ») au lieu d'un compteur */
  badgeTodo?: string;
  /** badge affiché en rouge si à traiter en priorité */
  urgent?: boolean;
  /** fonction non encore implémentée → page placeholder */
  soon?: boolean;
}

export const ACCUEIL: NavItem = {
  id: 'accueil',
  label: 'Accueil',
  route: '/',
  icon: 'accueil',
};

export const LOT1: NavItem[] = [
  { id: 'depot', label: 'Dépôt Situation Financière', route: '/depot-situations', icon: 'depot', badgeTodo: 'À faire', soon: true },
  { id: 'attestations', label: 'Attestations', route: '/attestations', icon: 'attestations', badgeKey: 'attestations', soon: true },
  { id: 'cheques', label: 'Suivi des chèques', route: '/cheques', icon: 'cheques', badgeKey: 'chequesEnAttente', urgent: true },
  { id: 'bureau', label: "Envois bureau d'ordre", route: '/bureau-ordre', icon: 'bureau', badgeKey: 'bureauOrdre', soon: true },
  { id: 'cotation', label: 'Demande de cotation', route: '/cotation', icon: 'cotation', badgeKey: 'cotations' },
  { id: 'expertise', label: "Demande d'expertise", route: '/expertise', icon: 'expertise', badgeKey: 'expertises', soon: true },
];

export const LOT2: NavItem[] = [
  { id: 'accords', label: "Accords d'échéancier", route: '/accords-echeancier', icon: 'accords', badgeKey: 'accordsEcheancier', soon: true },
  { id: 'echeanciers', label: 'Suivi des échéanciers', route: '/echeanciers', icon: 'echeanciers', badgeKey: 'echeanciersRisque', urgent: true, soon: true },
  { id: 'contentieux', label: 'Créances & contentieux', route: '/contentieux', icon: 'contentieux', badgeKey: 'contentieux', urgent: true, soon: true },
];

/** Tous les items routables (pour générer les routes placeholder). */
export const ITEMS_SOON: NavItem[] = [...LOT1, ...LOT2].filter((i) => i.soon);
