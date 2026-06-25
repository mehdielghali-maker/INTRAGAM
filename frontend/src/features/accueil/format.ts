import { Variation } from './types';

const NF = new Intl.NumberFormat('fr-FR');
const NF1 = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/**
 * Entier avec un POINT comme séparateur de milliers (convention demandée). fr-FR insère une
 * espace fine insécable entre les milliers ; on la remplace par un point. Ex. 1.000.000
 * (\s couvre l'espace fine insécable U+202F et l'espace insécable U+00A0 en JS.)
 */
function grouperMilliers(valeur: number): string {
  return NF.format(Math.round(valeur)).replace(/\s/g, '.');
}

/** Entier avec séparateur de milliers en point. Ex. 1.248 */
export function nombre(valeur: number): string {
  return grouperMilliers(valeur);
}

/** Montant en DA avec séparateur de milliers en point. Ex. 18.540.000 DA */
export function montantDA(valeur: number): string {
  return `${grouperMilliers(valeur)} DA`;
}

/** Ratio en pourcentage. Ex. 68,4 % */
export function ratioPct(valeur: number): string {
  return `${NF1.format(valeur)} %`;
}

/** Libellé signé d'une variation (puce de delta). Ex. +7,9 % / −2,8 pts */
export function variationLabel(v: Variation): string {
  const signe = v.valeur > 0 ? '+' : v.valeur < 0 ? '−' : '';
  const abs = NF1.format(Math.abs(v.valeur));
  const unite = v.unite === 'POINTS' ? ' pts' : ' %';
  return `${signe}${abs}${unite}`;
}
