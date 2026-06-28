import { describe, expect, it } from 'vitest';
import { decsin, Declaration, rattacherDeclarationsEnAttente } from '@decsin';

function declarationEnAttente(code: string): Declaration {
  return {
    idLocal: `local-${code}`, code, origine: 'AGA', statut: 'A_VALIDER', aRattacher: true,
    immatriculation: '09876-116-16', marque: 'Renault Clio', numPolice: 'P-1', conducteur: 'K. Meziane',
    dateSinistre: '2026-06-25', heureSinistre: '10:00', lieuSinistre: 'Alger', observations: 'Test',
    blesses: false, pieces: [],
  };
}

describe('validation différée hors-ligne (rattachement à la reconnexion)', () => {
  it('rattache à PROASSUR les déclarations « à rattacher » et leur attribue un N° sinistre', async () => {
    const code = 'DEC-TEST-2026';
    await decsin.creerDeclaration(declarationEnAttente(code));

    const n = await rattacherDeclarationsEnAttente();
    expect(n).toBeGreaterThanOrEqual(1);

    const apres = await decsin.getDeclarationParCode(code);
    expect(apres?.statut).toBe('VALIDEE');
    expect(apres?.numSinistre).toBeTruthy();
    expect(apres?.aRattacher).toBe(false);
  });

  it('ne touche pas une déclaration « à valider » sans drapeau de rattachement', async () => {
    const code = 'DEC-NOFLAG-2026';
    await decsin.creerDeclaration({ ...declarationEnAttente(code), aRattacher: false });

    await rattacherDeclarationsEnAttente();

    const apres = await decsin.getDeclarationParCode(code);
    expect(apres?.statut).toBe('A_VALIDER');
    expect(apres?.numSinistre).toBeUndefined();
  });
});
