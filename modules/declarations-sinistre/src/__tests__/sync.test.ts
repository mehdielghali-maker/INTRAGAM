import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mocks de la base locale et de l'adaptateur DECSIN (pas d'IndexedDB ni de réseau en test).
const ordre: string[] = [];

vi.mock('../offline/db', () => ({
  declarationsASynchroniser: vi.fn(),
  piecesDe: vi.fn(),
  majStatutSync: vi.fn(async (...a: unknown[]) => { ordre.push(`maj:${a[1]}`); }),
}));

vi.mock('@decsin', () => ({
  decsin: {
    postFile: vi.fn(async (_blob: Blob, type: string) => { ordre.push(`post:${type}`); return { type, reference: `ref-${type}` }; }),
    saveDeclaration: vi.fn(async () => { ordre.push('save'); }),
  },
}));

import { decsin } from '@decsin';
import { declarationsASynchroniser, majStatutSync, piecesDe } from '../offline/db';
import { synchroniser } from '../offline/sync';

const decl = { idLocal: 'd1', code: 'C1', pieces: [], statutSync: 'en_attente' };
const pieces = [
  { id: 'p1', type: 'recto_constat', blob: new Blob(['x']) },
  { id: 'p2', type: 'face_avant', blob: new Blob(['y']) },
];

describe('synchronisation (flux 2 temps)', () => {
  beforeEach(() => {
    ordre.length = 0;
    vi.clearAllMocks();
    (declarationsASynchroniser as ReturnType<typeof vi.fn>).mockResolvedValue([decl]);
    (piecesDe as ReturnType<typeof vi.fn>).mockResolvedValue(pieces);
  });

  it('envoie chaque pièce PUIS la déclaration, et marque « synchronise »', async () => {
    const res = await synchroniser();

    expect((decsin.postFile as ReturnType<typeof vi.fn>)).toHaveBeenCalledTimes(2);
    expect((decsin.saveDeclaration as ReturnType<typeof vi.fn>)).toHaveBeenCalledTimes(1);
    // Ordre : les 2 pièces avant la déclaration.
    expect(ordre).toEqual(['post:recto_constat', 'post:face_avant', 'save', 'maj:synchronise']);
    // La déclaration est sauvegardée avec les 2 références.
    const refs = (decsin.saveDeclaration as ReturnType<typeof vi.fn>).mock.calls[0][1];
    expect(refs).toHaveLength(2);
    expect(res.synchronisees).toBe(1);
  });

  it('marque « erreur » si l\'envoi d\'une pièce échoue (déclaration non sauvegardée)', async () => {
    (decsin.postFile as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('réseau'));

    const res = await synchroniser();

    expect((decsin.saveDeclaration as ReturnType<typeof vi.fn>)).not.toHaveBeenCalled();
    expect(majStatutSync).toHaveBeenCalledWith('d1', 'erreur', expect.any(String));
    expect(res.echecs).toBe(1);
  });
});
