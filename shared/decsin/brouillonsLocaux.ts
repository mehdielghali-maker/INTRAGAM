// Store LOCAL des brouillons de déclaration (présentiel). Un brouillon vit en local (localStorage)
// tant qu'il n'est pas VALIDÉ : il n'est JAMAIS envoyé à DECSIN/PROASSUR avant la validation. Vrai
// en mode mock ET réel (DECSIN ne gère pas forcément les brouillons — [DSI à confirmer]). Implémente
// l'interface BrouillonStore du socle @dossier.

import type { BrouillonStore } from '../dossier/brouillon';
import { Declaration } from './types';
import { identifiant } from './media';

const CLE = 'decsin.brouillons';

function lire(): Declaration[] {
  try {
    const brut = localStorage.getItem(CLE);
    if (brut) {
      return JSON.parse(brut) as Declaration[];
    }
  } catch {
    /* localStorage indisponible */
  }
  const demo = brouillonsDemo();
  ecrire(demo);
  return demo;
}

function ecrire(liste: Declaration[]): void {
  try {
    localStorage.setItem(CLE, JSON.stringify(liste));
  } catch {
    /* quota / mode privé */
  }
}

/** Un brouillon présentiel de démo (saisie AGA en cours, incomplète). */
function brouillonsDemo(): Declaration[] {
  return [
    {
      idLocal: identifiant('d'),
      code: 'DEC-BR01-2026',
      origine: 'AGA',
      statut: 'BROUILLON',
      immatriculation: '09876-116-16',
      marque: 'Renault Clio',
      numPolice: 'P-2024-00871',
      conducteur: 'Karim Meziane',
      dateSinistre: '2026-06-27',
      heureSinistre: '08:20',
      lieuSinistre: 'Bd des Martyrs, Alger',
      observations: 'Accrochage en stationnement (saisie en cours).',
      blesses: false,
      pieces: [],
      dateSaisie: '2026-06-27',
    },
  ];
}

/** Brouillons présentiel — persistance LOCALE, idempotente par idLocal, sans aucun envoi backend. */
export const brouillonsLocaux: BrouillonStore<Declaration> = {
  async lister() {
    return lire();
  },
  async obtenir(idLocal) {
    return lire().find((d) => d.idLocal === idLocal) ?? null;
  },
  async enregistrer(dossier) {
    const all = lire();
    const i = all.findIndex((d) => d.idLocal === dossier.idLocal);
    if (i >= 0) {
      all[i] = dossier;
    } else {
      all.unshift(dossier);
    }
    ecrire(all);
    return dossier;
  },
  async supprimer(idLocal) {
    ecrire(lire().filter((d) => d.idLocal !== idLocal));
  },
};
