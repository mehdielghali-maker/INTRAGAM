import { beforeEach, describe, expect, it, vi } from 'vitest';

const ordre: string[] = [];

vi.mock('../offline/db', () => ({
  souscriptionsASynchroniser: vi.fn(),
  piecesDe: vi.fn(),
  majStatutSync: vi.fn(async (...a: unknown[]) => { ordre.push(`maj:${a[1]}`); }),
}));

vi.mock('@souscription', () => ({
  souscription: {
    postFile: vi.fn(async (_blob: Blob, type: string) => { ordre.push(`post:${type}`); return { type, reference: `ref-${type}` }; }),
    enregistrerSouscription: vi.fn(async () => { ordre.push('save'); }),
  },
}));

import { souscription } from '@souscription';
import { majStatutSync, piecesDe, souscriptionsASynchroniser } from '../offline/db';
import { synchroniser } from '../offline/sync';

const s = { idLocal: 's1', reference: 'SCR-1', pieces: [], statutSync: 'en_attente' };
const pieces = [
  { id: 'p1', type: 'cni_recto', blob: new Blob(['x']) },
  { id: 'p2', type: 'carte_grise', blob: new Blob(['y']) },
];

describe('synchronisation souscription (flux 2 temps)', () => {
  beforeEach(() => {
    ordre.length = 0;
    vi.clearAllMocks();
    (souscriptionsASynchroniser as ReturnType<typeof vi.fn>).mockResolvedValue([s]);
    (piecesDe as ReturnType<typeof vi.fn>).mockResolvedValue(pieces);
  });

  it('envoie chaque pièce PUIS enregistre la souscription, et marque « synchronise »', async () => {
    const res = await synchroniser();
    expect((souscription.postFile as ReturnType<typeof vi.fn>)).toHaveBeenCalledTimes(2);
    expect((souscription.enregistrerSouscription as ReturnType<typeof vi.fn>)).toHaveBeenCalledTimes(1);
    expect(ordre).toEqual(['post:cni_recto', 'post:carte_grise', 'save', 'maj:synchronise']);
    const refs = (souscription.enregistrerSouscription as ReturnType<typeof vi.fn>).mock.calls[0][1];
    expect(refs).toHaveLength(2);
    expect(res.synchronisees).toBe(1);
  });

  it('marque « erreur » si l\'envoi d\'une pièce échoue', async () => {
    (souscription.postFile as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('réseau'));
    const res = await synchroniser();
    expect((souscription.enregistrerSouscription as ReturnType<typeof vi.fn>)).not.toHaveBeenCalled();
    expect(majStatutSync).toHaveBeenCalledWith('s1', 'erreur', expect.any(String));
    expect(res.echecs).toBe(1);
  });
});
