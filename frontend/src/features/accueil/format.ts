import { Variation } from './types';

const NF = new Intl.NumberFormat('fr-FR');
const NF1 = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** Montant en DA avec séparateur de milliers (locale fr). Ex. 18 540 000 DA */
export function montantDA(valeur: number): string {
  return `${NF.format(Math.round(valeur))} DA`;
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
