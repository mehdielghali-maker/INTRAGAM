import { describe, expect, it } from 'vitest';
import {
  cleLot,
  compterAttestations,
  contratVersChamps,
  estDoublonPolice,
  libelleMois,
  lignePourEnvoi,
  listerMoisSelectionnables,
  moisCourant,
  parsePrime,
  proposerStatutLigne,
  reduireStatutLigne,
  totalPrimeTTC,
} from '../logiqueReleve';
import { ContratLecture, LigneReleve } from '../types';

function ligne(partiel: Partial<LigneReleve>): LigneReleve {
  return {
    id: 'l1',
    mois: '2026-06',
    agence: '40.AR.0105',
    numeroPolice: '',
    numeroQuittance: '',
    immatriculation: '',
    assure: '',
    valideDu: '',
    valideAu: '',
    primeTTC: '',
    codeAgence: '',
    statutLigne: 'confirme',
    dateAjout: 0,
    ...partiel,
  };
}

describe('clé de lot (un lot par agence active + mois)', () => {
  it('concatène agence|AAAA-MM', () => {
    expect(cleLot('40.AR.0105', '2026-06')).toBe('40.AR.0105|2026-06');
  });
});

describe('parsePrime (format français, virgule décimale)', () => {
  it('lit une prime avec virgule', () => {
    expect(parsePrime('2400,97')).toBeCloseTo(2400.97);
  });
  it('tolère les espaces de milliers (y compris insécables)', () => {
    expect(parsePrime('2 400,97')).toBeCloseTo(2400.97);
    expect(parsePrime('3 150,00')).toBeCloseTo(3150);
  });
  it('renvoie 0 si absente ou illisible', () => {
    expect(parsePrime(null)).toBe(0);
    expect(parsePrime('')).toBe(0);
    expect(parsePrime('abc')).toBe(0);
  });
});

describe('total prime TTC cumulée', () => {
  const lignes = [
    ligne({ id: 'a', primeTTC: '2 130,00', statutLigne: 'confirme' }),
    ligne({ id: 'b', primeTTC: '3 000,50', statutLigne: 'a_verifier' }),
    ligne({ id: 'c', primeTTC: '2 400,97', statutLigne: 'a_lire' }), // pas encore ajoutée
    ligne({ id: 'd', primeTTC: '1 000,00', statutLigne: 'lu' }), // pas encore confirmée
  ];
  it('ne cumule que les lignes AJOUTÉES (confirmées ou à vérifier)', () => {
    expect(totalPrimeTTC(lignes)).toBeCloseTo(5130.5);
    expect(compterAttestations(lignes)).toBe(2);
  });
});

describe('détection de doublon de n° de police dans le lot', () => {
  const lignes = [
    ligne({ id: 'a', numeroPolice: '407020091260318' }),
    ligne({ id: 'b', numeroPolice: '407020091260559' }),
    ligne({ id: 'c', numeroPolice: '' }),
  ];
  it('détecte un numéro déjà présent', () => {
    expect(estDoublonPolice(lignes, '407020091260318')).toBe(true);
  });
  it('ignore la ligne elle-même (édition)', () => {
    expect(estDoublonPolice(lignes, '407020091260318', 'a')).toBe(false);
  });
  it('ignore les numéros vides', () => {
    expect(estDoublonPolice(lignes, '')).toBe(false);
    expect(estDoublonPolice(lignes, '   ')).toBe(false);
  });
});

describe('réduction des statuts de ligne', () => {
  it('a_lire + ocr_reussi → lu ; échec → reste a_lire (file idempotente)', () => {
    expect(reduireStatutLigne('a_lire', 'ocr_reussi')).toBe('lu');
    expect(reduireStatutLigne('a_lire', 'ocr_echec')).toBe('a_lire');
  });
  it('ajout → confirme ou a_verifier', () => {
    expect(reduireStatutLigne('lu', 'ajout_confirme')).toBe('confirme');
    expect(reduireStatutLigne('lu', 'ajout_a_verifier')).toBe('a_verifier');
  });
  it('rescan → repart en lecture, quel que soit le statut', () => {
    expect(reduireStatutLigne('confirme', 'rescan')).toBe('a_lire');
    expect(reduireStatutLigne('a_verifier', 'rescan')).toBe('a_lire');
  });
  it('ocr_reussi ne rétrograde pas une ligne déjà ajoutée', () => {
    expect(reduireStatutLigne('confirme', 'ocr_reussi')).toBe('confirme');
  });
});

describe('mapping contrat OCR → ligne', () => {
  const contrat: ContratLecture = {
    numeroPolice: '407020091260318',
    numeroQuittance: '06681947',
    immatriculation: '631220 108 16',
    assure: 'B. Kaddour',
    valideDu: '01/06/2026',
    valideAu: '31/05/2027',
    primeTTC: '3150,00',
    codeAgence: '40.AR.0105',
    confiance: 0.96,
    statut: 'lu',
    texteBrut: '…',
  };
  it('reprend tous les champs et remplace null par une chaîne vide', () => {
    expect(contratVersChamps(contrat)).toEqual({
      numeroPolice: '407020091260318',
      numeroQuittance: '06681947',
      immatriculation: '631220 108 16',
      assure: 'B. Kaddour',
      valideDu: '01/06/2026',
      valideAu: '31/05/2027',
      primeTTC: '3150,00',
      codeAgence: '40.AR.0105',
    });
    const vide = contratVersChamps({ ...contrat, numeroPolice: null, primeTTC: null });
    expect(vide.numeroPolice).toBe('');
    expect(vide.primeTTC).toBe('');
  });
});

describe('statut proposé à l’ajout', () => {
  const valides = { numeroPolice: '407020091260318', numeroQuittance: '06681947' };
  it('confirmé si numéros plausibles et OCR sûr', () => {
    expect(proposerStatutLigne(valides, 'lu')).toBe('confirme');
  });
  it('à vérifier si le n° de police n’a pas 15 chiffres', () => {
    expect(proposerStatutLigne({ ...valides, numeroPolice: '4070200912604' }, 'lu')).toBe('a_verifier');
  });
  it('à vérifier si la quittance n’a pas 8 chiffres', () => {
    expect(proposerStatutLigne({ ...valides, numeroQuittance: '123' }, 'lu')).toBe('a_verifier');
  });
  it('OCR douteux → à vérifier, sauf correction MANUELLE de l’AGA', () => {
    expect(proposerStatutLigne(valides, 'a_verifier')).toBe('a_verifier');
    expect(proposerStatutLigne(valides, 'a_verifier', true)).toBe('confirme');
  });
});

describe('mapping ligne → envoi API (statutLigne restreint au contrat figé)', () => {
  it('confirme reste confirme, tout le reste part en a_verifier', () => {
    expect(lignePourEnvoi(ligne({ statutLigne: 'confirme' })).statutLigne).toBe('confirme');
    expect(lignePourEnvoi(ligne({ statutLigne: 'a_verifier' })).statutLigne).toBe('a_verifier');
  });
  it('n’envoie pas les champs locaux (id, photo, statut interne)', () => {
    const envoi = lignePourEnvoi(
      ligne({ numeroPolice: '407020091260318', photoDataUrl: 'data:…', confiance: 0.9 }),
    );
    expect(envoi).not.toHaveProperty('id');
    expect(envoi).not.toHaveProperty('photoDataUrl');
    expect(envoi).not.toHaveProperty('confiance');
    expect(envoi.numeroPolice).toBe('407020091260318');
  });
});

describe('mois (libellés et sélection)', () => {
  it('libelleMois : 2026-06 → Juin 2026', () => {
    expect(libelleMois('2026-06')).toBe('Juin 2026');
    expect(libelleMois('2026-01')).toBe('Janvier 2026');
  });
  it('moisCourant : AAAA-MM avec zéro devant', () => {
    expect(moisCourant(new Date(2026, 6, 3))).toBe('2026-07');
    expect(moisCourant(new Date(2026, 0, 15))).toBe('2026-01');
  });
  it('listerMoisSelectionnables : mois courant en tête puis les précédents (année déroulée)', () => {
    const choix = listerMoisSelectionnables(new Date(2026, 0, 10), 3);
    expect(choix.map((c) => c.valeur)).toEqual(['2026-01', '2025-12', '2025-11']);
    expect(choix[0].libelle).toBe('Janvier 2026');
  });
});
