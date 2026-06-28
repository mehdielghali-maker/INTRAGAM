import { describe, it, expect, beforeEach } from 'vitest';
import { brouillonsLocaux, Declaration } from '@decsin';

function decl(idLocal: string): Declaration {
  return {
    idLocal,
    code: `DEC-${idLocal}`,
    origine: 'AGA',
    statut: 'BROUILLON',
    immatriculation: '09876-116-16',
    marque: 'Renault Clio',
    numPolice: 'P-1',
    conducteur: 'X',
    dateSinistre: '',
    heureSinistre: '',
    lieuSinistre: '',
    observations: '',
    blesses: false,
    pieces: [],
  };
}

describe('brouillonsLocaux (déclaration) — persistance LOCALE, idempotente, sans backend', () => {
  beforeEach(() => localStorage.clear());

  it('enregistre (upsert par idLocal), retrouve et supprime un brouillon', async () => {
    await brouillonsLocaux.enregistrer(decl('b1'));
    await brouillonsLocaux.enregistrer(decl('b1')); // upsert : pas de doublon
    const liste = await brouillonsLocaux.lister();
    expect(liste.filter((d) => d.idLocal === 'b1')).toHaveLength(1);

    const repris = await brouillonsLocaux.obtenir('b1');
    expect(repris?.statut).toBe('BROUILLON');

    await brouillonsLocaux.supprimer('b1');
    expect(await brouillonsLocaux.obtenir('b1')).toBeNull();
  });
});
