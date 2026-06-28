// Mock réaliste de DECSIN + PROASSUR pour développer sans dépendre du backend.
// Persiste en localStorage (par origine) pour conserver les déclarations entre rechargements.
// Le code SMS de démo (double facteur) est CODE_OTP_DEMO.

import { config } from './config';
import { DecsinPort } from './decsinPort';
import { identifiant } from './media';
import { Declaration, FiltreDeclaration, ReferenceFichier, Vehicule } from './types';

/** Code SMS accepté par le mock (en réel : OTP envoyé par DECSIN). */
export const CODE_OTP_DEMO = '0000';

const CLE_STORE = 'decsin.mock.declarations';

// Véhicules de démo connus par immatriculation (sinon, repli générique).
const VEHICULES: Record<string, Vehicule> = {
  '09876-116-16': { id: 'v1', immatriculation: '09876-116-16', marque: 'Renault Clio', numPolice: 'P-2024-00871', conducteur: 'Karim Meziane' } as Vehicule,
  '12345-114-31': { id: 'v2', immatriculation: '12345-114-31', marque: 'Peugeot 208', numPolice: 'P-2023-04412', conducteur: 'Sofiane Brahimi' } as Vehicule,
};

function lireStore(): Declaration[] {
  try {
    const brut = localStorage.getItem(CLE_STORE);
    if (brut) {
      return JSON.parse(brut) as Declaration[];
    }
  } catch {
    /* localStorage indisponible : on retombe sur les données de démo */
  }
  const demo = declarationsDemo();
  ecrireStore(demo);
  return demo;
}

function ecrireStore(declarations: Declaration[]): void {
  try {
    localStorage.setItem(CLE_STORE, JSON.stringify(declarations));
  } catch {
    /* ignore (mode privé, quota) */
  }
}

function maintenant(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Génère un code DEC-XXXX-AAAA (préfixe configurable). */
export function genererCode(): string {
  const hex = Math.floor(Math.random() * 0xffff)
    .toString(16)
    .toUpperCase()
    .padStart(4, '0');
  return `${config.codePrefixe}-${hex}-${new Date().getFullYear()}`;
}

function declarationsDemo(): Declaration[] {
  return [
    {
      idLocal: identifiant('d'), code: 'DEC-4A21-2026', origine: 'AGA', statut: 'A_VALIDER',
      immatriculation: '09876-116-16', marque: 'Renault Clio', numPolice: 'P-2024-00871', conducteur: 'Karim Meziane',
      dateSinistre: '2026-06-22', heureSinistre: '14:30', lieuSinistre: 'RN5, Rouiba, Alger',
      observations: 'Choc arrière à un feu rouge.', blesses: false, telAssure: '0550 12 34 56', pieces: [],
      dateSaisie: '2026-06-22',
    },
    {
      idLocal: identifiant('d'), code: 'DEC-7F3A-2026', origine: 'CLIENT', statut: 'LIEN_ENVOYE',
      immatriculation: '12345-114-31', marque: 'Peugeot 208', numPolice: 'P-2023-04412', conducteur: 'Sofiane Brahimi',
      dateSinistre: '', heureSinistre: '', lieuSinistre: '', observations: '', blesses: false, pieces: [],
      client: { nom: 'Sofiane Brahimi', telephone: '0661 22 33 44' },
    },
    {
      idLocal: identifiant('d'), code: 'DEC-1C90-2026', origine: 'AGA', statut: 'VALIDEE',
      immatriculation: '09876-116-16', marque: 'Renault Clio', numPolice: 'P-2024-00871', conducteur: 'Karim Meziane',
      dateSinistre: '2026-06-10', heureSinistre: '09:10', lieuSinistre: 'Bd Krim Belkacem, Alger',
      observations: 'Rayure sur parking.', blesses: false, pieces: [], numSinistre: 'SIN-2026-118324', dateSaisie: '2026-06-10',
    },
    {
      idLocal: identifiant('d'), code: 'DEC-9B57-2026', origine: 'CLIENT', statut: 'INCOMPLETE',
      immatriculation: '12345-114-31', marque: 'Peugeot 208', numPolice: 'P-2023-04412', conducteur: 'Sofiane Brahimi',
      dateSinistre: '2026-06-18', heureSinistre: '17:45', lieuSinistre: 'Autoroute Est, Boumerdès',
      observations: 'Collision avec un tiers.', blesses: false, compagnieAdverse: 'CAAR', vehiculeAdverse: '55512-110-35',
      pieces: [], client: { nom: 'Sofiane Brahimi', telephone: '0661 22 33 44' },
    },
  ];
}

/** Implémentation MOCK du port (dev). */
export const decsinMock: DecsinPort = {
  async prefillVehiculeByImmat(immatriculation) {
    const cle = immatriculation.trim();
    if (VEHICULES[cle]) {
      return VEHICULES[cle];
    }
    if (!cle) {
      return null;
    }
    // Repli : véhicule générique pour toute immatriculation saisie.
    return { id: identifiant('v'), immatriculation: cle, marque: 'Véhicule assuré', numPolice: 'P-MOCK-0000' };
  },

  async rattacherDeclaration(_declaration) {
    const numSinistre = `SIN-${new Date().getFullYear()}-${Math.floor(100000 + Math.random() * 899999)}`;
    return { numSinistre, idDossierSinistre: identifiant('dossier') };
  },

  async loginConducteur(_telephone, code) {
    if (code !== CODE_OTP_DEMO) {
      return { code: 401, message: 'Code incorrect', codeClient: '', idConducteur: '', nomConducteur: '', vehicules: [] };
    }
    return {
      code: 0, message: 'OK', codeClient: 'CLI-MOCK', idConducteur: 'COND-MOCK',
      nomConducteur: 'Client GAM', vehicules: Object.values(VEHICULES),
    };
  },

  async getDeclarations(filtre?: FiltreDeclaration) {
    const all = lireStore();
    return filtre?.statut ? all.filter((d) => d.statut === filtre.statut) : all;
  },

  async getDeclarationParCode(code) {
    return lireStore().find((d) => d.code === code) ?? null;
  },

  async creerDeclaration(declaration) {
    const all = lireStore();
    const enregistree: Declaration = { ...declaration, dateSaisie: declaration.dateSaisie ?? maintenant() };
    const i = all.findIndex((d) => d.idLocal === declaration.idLocal || d.code === declaration.code);
    if (i >= 0) {
      all[i] = enregistree; // idempotence
    } else {
      all.unshift(enregistree);
    }
    ecrireStore(all);
    return enregistree;
  },

  async postFile(_blob, type, codeDeclaration) {
    // Référence simulée renvoyée par PostAldFile.
    const ref: ReferenceFichier = { type, reference: `ref-${codeDeclaration}-${identifiant('f')}` };
    return ref;
  },

  async saveDeclaration(declaration, references) {
    const all = lireStore();
    const pieces = declaration.pieces.map((p) => {
      const ref = references.find((r) => r.type === p.type);
      return ref ? { ...p, reference: ref.reference } : p;
    });
    // Une déclaration remplie passe « À valider » (sauf si déjà validée).
    const statut = declaration.statut === 'VALIDEE' ? 'VALIDEE' : 'A_VALIDER';
    const enregistree: Declaration = { ...declaration, pieces, statut };
    const i = all.findIndex((d) => d.idLocal === declaration.idLocal || d.code === declaration.code);
    if (i >= 0) {
      all[i] = enregistree;
    } else {
      all.unshift(enregistree);
    }
    ecrireStore(all);
    return enregistree;
  },
};
