// LOGIQUE PURE du relevé d'attestations (aucune dépendance navigateur/IndexedDB) :
// clé de lot, parsing des primes (virgule), total cumulé, détection de doublon, réduction des
// statuts de ligne et mapping contrat OCR → ligne. Testée unitairement (logiqueReleve.test.ts).

import { ChampsLigne, ContratLecture, LigneEnvoi, LigneReleve, StatutLigne } from './types';

/** N° de police attendu : exactement 15 chiffres. */
export const POLICE_VALIDE = /^\d{15}$/;
/** N° de quittance attendu : exactement 8 chiffres. */
export const QUITTANCE_VALIDE = /^\d{8}$/;

/** Clé du lot local : UN relevé par (agence active, mois). */
export function cleLot(agence: string, mois: string): string {
  return `${agence}|${mois}`;
}

/**
 * Convertit une prime « à la française » (« 2 400,97 », « 2400,97 ») en nombre.
 * Tolère les espaces (y compris insécables) et renvoie 0 si illisible/absente.
 */
export function parsePrime(prime: string | null | undefined): number {
  if (!prime) return 0;
  const nettoye = prime.replace(/[\s  ]/g, '').replace(',', '.');
  const n = Number.parseFloat(nettoye);
  return Number.isFinite(n) ? n : 0;
}

/** Formate un montant pour l'affichage (« 7 531,47 »). */
export function formaterMontant(n: number): string {
  return n.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** Une ligne compte dans le relevé dès qu'elle a été AJOUTÉE (confirmée ou à vérifier). */
export function estComptabilisee(statut: StatutLigne): boolean {
  return statut === 'confirme' || statut === 'a_verifier';
}

/** Nombre d'attestations ajoutées au relevé (les lignes encore en lecture ne comptent pas). */
export function compterAttestations(lignes: Pick<LigneReleve, 'statutLigne'>[]): number {
  return lignes.filter((l) => estComptabilisee(l.statutLigne)).length;
}

/** Total « Prime TTC cumulée » des lignes ajoutées au relevé. */
export function totalPrimeTTC(lignes: Pick<LigneReleve, 'primeTTC' | 'statutLigne'>[]): number {
  return lignes
    .filter((l) => estComptabilisee(l.statutLigne))
    .reduce((somme, l) => somme + parsePrime(l.primeTTC), 0);
}

/**
 * Détecte un DOUBLON de numéro de police dans le lot (garde-fou : même attestation photographiée
 * deux fois). Ignore la ligne elle-même (édition) et les numéros vides.
 */
export function estDoublonPolice(
  lignes: Pick<LigneReleve, 'id' | 'numeroPolice'>[],
  numeroPolice: string,
  idIgnore?: string,
): boolean {
  const cherche = numeroPolice.trim();
  if (!cherche) return false;
  return lignes.some((l) => l.id !== idIgnore && l.numeroPolice.trim() === cherche);
}

/** Événements qui font évoluer le statut d'une ligne. */
export type EvenementLigne =
  | 'ocr_reussi' // la lecture OCR a abouti → en attente de confirmation
  | 'ocr_echec' // hors-ligne ou échec → reste en file
  | 'ajout_confirme' // l'AGA ajoute la ligne, lecture sûre
  | 'ajout_a_verifier' // l'AGA ajoute la ligne malgré un doute
  | 'rescan'; // nouvelle photo → repart en lecture

/**
 * Réduction du statut d'une ligne : (statut, événement) → statut suivant.
 * Machine volontairement permissive sur l'édition (une ligne ajoutée reste modifiable
 * tant que le relevé n'est pas validé — le verrou est porté par le RELEVÉ, pas la ligne).
 */
export function reduireStatutLigne(statut: StatutLigne, evenement: EvenementLigne): StatutLigne {
  switch (evenement) {
    case 'ocr_reussi':
      return statut === 'a_lire' ? 'lu' : statut;
    case 'ocr_echec':
      return statut; // la ligne reste 'a_lire' (file idempotente, retentera)
    case 'ajout_confirme':
      return 'confirme';
    case 'ajout_a_verifier':
      return 'a_verifier';
    case 'rescan':
      return 'a_lire';
  }
}

/** Champs vides (ligne créée à la photo, avant lecture OCR). */
export function champsVides(): ChampsLigne {
  return {
    numeroPolice: '',
    numeroQuittance: '',
    immatriculation: '',
    assure: '',
    valideDu: '',
    valideAu: '',
    primeTTC: '',
    codeAgence: '',
  };
}

/** Mapping contrat OCR → champs éditables de la ligne (null → chaîne vide). */
export function contratVersChamps(contrat: ContratLecture): ChampsLigne {
  return {
    numeroPolice: contrat.numeroPolice ?? '',
    numeroQuittance: contrat.numeroQuittance ?? '',
    immatriculation: contrat.immatriculation ?? '',
    assure: contrat.assure ?? '',
    valideDu: contrat.valideDu ?? '',
    valideAu: contrat.valideAu ?? '',
    primeTTC: contrat.primeTTC ?? '',
    codeAgence: contrat.codeAgence ?? '',
  };
}

/**
 * Statut proposé à l'AJOUT au relevé : « confirmé » seulement si les numéros clés sont plausibles
 * ET que l'OCR n'a pas levé de doute — sauf correction MANUELLE de l'AGA (qui fait foi).
 */
export function proposerStatutLigne(
  champs: Pick<ChampsLigne, 'numeroPolice' | 'numeroQuittance'>,
  statutContrat: 'lu' | 'a_verifier',
  corrigeManuellement = false,
): 'confirme' | 'a_verifier' {
  const numerosValides =
    POLICE_VALIDE.test(champs.numeroPolice.trim()) && QUITTANCE_VALIDE.test(champs.numeroQuittance.trim());
  if (!numerosValides) return 'a_verifier';
  if (statutContrat === 'a_verifier' && !corrigeManuellement) return 'a_verifier';
  return 'confirme';
}

/** Mapping ligne locale → ligne du POST /api/attestations/releves (API FIGÉE). */
export function lignePourEnvoi(ligne: LigneReleve): LigneEnvoi {
  return {
    numeroPolice: ligne.numeroPolice,
    numeroQuittance: ligne.numeroQuittance,
    immatriculation: ligne.immatriculation,
    assure: ligne.assure,
    valideDu: ligne.valideDu,
    valideAu: ligne.valideAu,
    primeTTC: ligne.primeTTC,
    codeAgence: ligne.codeAgence,
    statutLigne: ligne.statutLigne === 'confirme' ? 'confirme' : 'a_verifier',
  };
}

/** Noms de mois FIXES (pas de dépendance à l'ICU de l'environnement). */
const MOIS_FR = [
  'Janvier',
  'Février',
  'Mars',
  'Avril',
  'Mai',
  'Juin',
  'Juillet',
  'Août',
  'Septembre',
  'Octobre',
  'Novembre',
  'Décembre',
];

/** « 2026-06 » → « Juin 2026 » (libellé de la barre de mois). */
export function libelleMois(mois: string): string {
  const [annee, numero] = mois.split('-');
  const index = Number.parseInt(numero, 10) - 1;
  if (!annee || index < 0 || index > 11 || Number.isNaN(index)) return mois;
  return `${MOIS_FR[index]} ${annee}`;
}

/** Mois courant au format AAAA-MM. */
export function moisCourant(reference: Date = new Date()): string {
  const m = reference.getMonth() + 1;
  return `${reference.getFullYear()}-${String(m).padStart(2, '0')}`;
}

/** Mois sélectionnables : le mois courant puis les N précédents (relevés de production). */
export function listerMoisSelectionnables(
  reference: Date = new Date(),
  nombre = 6,
): { valeur: string; libelle: string }[] {
  const resultat: { valeur: string; libelle: string }[] = [];
  for (let i = 0; i < nombre; i++) {
    const d = new Date(reference.getFullYear(), reference.getMonth() - i, 1);
    const valeur = moisCourant(d);
    resultat.push({ valeur, libelle: libelleMois(valeur) });
  }
  return resultat;
}
