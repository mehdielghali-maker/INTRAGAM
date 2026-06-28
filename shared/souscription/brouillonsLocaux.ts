// Store LOCAL des brouillons de souscription (présentiel). Comme pour la déclaration, un brouillon
// vit en local (localStorage) tant qu'il n'est pas VALIDÉ : il n'est JAMAIS envoyé à GAM/SecGam avant
// la validation. Implémente l'interface BrouillonStore du socle @dossier.

import type { BrouillonStore } from '../dossier/brouillon';
import { identifiant } from '@sinistre-ui';
import { Souscription } from './types';

const CLE = 'souscription.brouillons';

function lire(): Souscription[] {
  try {
    const brut = localStorage.getItem(CLE);
    if (brut) {
      return JSON.parse(brut) as Souscription[];
    }
  } catch {
    /* localStorage indisponible */
  }
  const demo = brouillonsDemo();
  ecrire(demo);
  return demo;
}

function ecrire(liste: Souscription[]): void {
  try {
    localStorage.setItem(CLE, JSON.stringify(liste));
  } catch {
    /* quota / mode privé */
  }
}

/** Un brouillon présentiel de démo (souscription AGA en cours, incomplète). */
function brouillonsDemo(): Souscription[] {
  return [
    {
      idLocal: identifiant('s'),
      reference: 'SCR-BR01-2026',
      typeProduit: 'AUTO',
      codeBranche: '13',
      libelleBranche: 'Automobile',
      codeSousBranche: '131',
      libelleSousBranche: 'Auto particulier',
      numeroPolice: 'AUTO-2026-00123',
      nomClient: 'Karim Meziane',
      origine: 'AGA',
      assure: { prenom: 'Karim', nom: 'Meziane', telephone: '0550 22 33 44' },
      vehicule: { immatriculation: '09876-116-16', marque: 'Renault Clio' },
      pieces: [],
      statut: 'BROUILLON',
      dateSaisie: '2026-06-27',
      agence: { code: '02.1.SAID', nom: 'Agence Saïd Hamdine' },
    },
  ];
}

/** Brouillons présentiel — persistance LOCALE, idempotente par idLocal, sans aucun envoi backend. */
export const brouillonsLocaux: BrouillonStore<Souscription> = {
  async lister() {
    return lire();
  },
  async obtenir(idLocal) {
    return lire().find((s) => s.idLocal === idLocal) ?? null;
  },
  async enregistrer(dossier) {
    const all = lire();
    const i = all.findIndex((s) => s.idLocal === dossier.idLocal);
    if (i >= 0) {
      all[i] = dossier;
    } else {
      all.unshift(dossier);
    }
    ecrire(all);
    return dossier;
  },
  async supprimer(idLocal) {
    ecrire(lire().filter((s) => s.idLocal !== idLocal));
  },
};
