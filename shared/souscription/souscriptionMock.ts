// Mock réaliste de la souscription auto (dev) — persiste en localStorage. Agent/police de démo,
// OTP de démo, OCR figé. Reproduit le principe de l'app GAM Expert sans dépendre du backend.

import { identifiant } from '@sinistre-ui';
import { config } from './config';
import { CriteresRecherche, SouscriptionPort } from './souscriptionPort';
import { ChampsOcr, Entite, FiltreSouscription, PreEntite, Souscription } from './types';

/** OTP accepté par le mock (en réel : OTP envoyé par le backend). */
export const CODE_OTP_AGENT = '0000';

const CLE = 'souscription.mock';

const ENTITES: Entite[] = [
  { numeroPolice: 'AUTO-2026-00123', nomClient: 'Karim Meziane', codeBranche: '13', libelleBranche: 'Automobile', codeSousBranche: '131', libelleSousBranche: 'Auto particulier', marque: 'Renault Clio', immatriculation: '09876-116-16' },
  { numeroPolice: 'AUTO-2026-00457', nomClient: 'Sofiane Brahimi', codeBranche: '13', libelleBranche: 'Automobile', codeSousBranche: '131', libelleSousBranche: 'Auto particulier', marque: 'Peugeot 208', immatriculation: '12345-114-31' },
];

function lireStore(): Souscription[] {
  try {
    const brut = localStorage.getItem(CLE);
    if (brut) {
      return JSON.parse(brut) as Souscription[];
    }
  } catch {
    /* indisponible */
  }
  return [];
}

function ecrireStore(liste: Souscription[]): void {
  try {
    localStorage.setItem(CLE, JSON.stringify(liste));
  } catch {
    /* quota / mode privé */
  }
}

function upsert(s: Souscription): Souscription {
  const all = lireStore();
  const i = all.findIndex((x) => x.idLocal === s.idLocal || x.reference === s.reference);
  if (i >= 0) {
    all[i] = s;
  } else {
    all.unshift(s);
  }
  ecrireStore(all);
  return s;
}

/** Génère une référence SCR-XXXX-AAAA (préfixe configurable). */
export function genererReference(): string {
  const hex = Math.floor(Math.random() * 0xffff).toString(16).toUpperCase().padStart(4, '0');
  return `${config.codePrefixe}-${hex}-${new Date().getFullYear()}`;
}

export const souscriptionMock: SouscriptionPort = {
  async loginAgent(_login, _motDePasse, otp) {
    if (otp && otp !== CODE_OTP_AGENT) {
      return { code: 401, message: 'Code incorrect', codeAgent: '', nomAgent: '' };
    }
    return { code: 0, message: 'OK', codeAgent: 'AG-MOCK', nomAgent: 'Agent GAM' };
  },

  async rechercheEntite(criteres: CriteresRecherche) {
    const np = criteres.numeroPolice?.trim().toLowerCase();
    const nom = criteres.nomClient?.trim().toLowerCase();
    return ENTITES.filter(
      (e) =>
        (!np || e.numeroPolice.toLowerCase().includes(np)) &&
        (!nom || e.nomClient.toLowerCase().includes(nom)) &&
        (!criteres.codeBranche || e.codeBranche === criteres.codeBranche),
    );
  },

  async listePreEntites() {
    const pe: PreEntite[] = [{ id: 'auto', libelle: 'Contrat automobile', typeProduit: 'AUTO' }];
    return pe;
  },

  async getOcrData(_imageBase64, _typeDoc): Promise<ChampsOcr> {
    // SEAM OCR : valeurs figées (à brancher sur initialiserEntitesOcrGAM + Dynamsoft).
    return { prenom: 'Karim', nom: 'Meziane', numero: '109876543', sexe: 'M', adresse: 'Alger' };
  },

  async getSouscriptions(filtre?: FiltreSouscription) {
    const all = lireStore();
    return filtre?.statut ? all.filter((s) => s.statut === filtre.statut) : all;
  },

  async getSouscriptionParReference(reference) {
    return lireStore().find((s) => s.reference === reference) ?? null;
  },

  async creerSouscription(s) {
    return upsert({ ...s, dateSaisie: s.dateSaisie ?? new Date().toISOString().slice(0, 10) });
  },

  async postFile(_blob, typeDoc, reference) {
    return { type: typeDoc, reference: `ref-${reference}-${identifiant('f')}` };
  },

  async enregistrerSouscription(souscription, references) {
    const pieces = souscription.pieces.map((p) => {
      const ref = references.find((r) => r.type === p.type);
      return ref ? { ...p, reference: ref.reference } : p;
    });
    // « Enregistrer » = VALIDER : la souscription est finalisée côté GAM (terminal, verrouillé).
    return upsert({ ...souscription, pieces, statut: 'VALIDEE' });
  },
};
