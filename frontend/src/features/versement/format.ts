// Formatage local (locale fr) pour le module « Versement bancaire ».

const NF = new Intl.NumberFormat('fr-FR');

/** Montant en DA avec séparateur de milliers (locale fr). Ex. 1 450 000 DA */
export function montantDA(valeur: number): string {
  return `${NF.format(Math.round(valeur))} DA`;
}

/** Date courte jj/mm depuis une date ISO. */
export function dateCourte(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
}

/** Date jj/mm/aaaa depuis une date ISO. */
export function dateLongue(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}

/** Date du jour au format ISO YYYY-MM-DD (pour le champ date par défaut). */
export function dateDuJourIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
