import { describe, it, expect, vi } from 'vitest';
import { verifierPlaque, construireResultat, reco } from '@reco';
import { recoHttp } from '@reco/recoHttp';
import type { ResultatReco } from '@reco';

const vehiculeAvecPlaque = (plaque: string | null): ResultatReco => ({
  estVehicule: true,
  typeVehicule: 'voiture',
  plaque,
  confiance: 0.92,
  confianceVehicule: 0.96,
});

describe('verifierPlaque (portage TS fidèle du service Java)', () => {
  it('plaque lue = immatriculation du contrat → CONFORME', () => {
    expect(verifierPlaque(vehiculeAvecPlaque('0987611616'), '0987611616', 'avant')).toBe('CONFORME');
  });

  it('normalise espaces/tirets des deux côtés avant comparaison', () => {
    expect(verifierPlaque(vehiculeAvecPlaque('09876 116 16'), '09876-116-16', 'arriere')).toBe('CONFORME');
  });

  it('plaque différente du contrat → NON_CONFORME', () => {
    expect(verifierPlaque(vehiculeAvecPlaque('0987611616'), '11111-222-33', 'avant')).toBe('NON_CONFORME');
  });

  it('vue avant sans plaque lisible → NON_LUE', () => {
    expect(verifierPlaque(vehiculeAvecPlaque(null), '0987611616', 'avant')).toBe('NON_LUE');
  });

  it('résultat absent → NON_LUE', () => {
    expect(verifierPlaque(null, '0987611616', 'avant')).toBe('NON_LUE');
  });

  it('photo sans véhicule → PAS_UN_VEHICULE', () => {
    const sansVehicule: ResultatReco = {
      estVehicule: false,
      typeVehicule: null,
      plaque: null,
      confiance: 0,
      confianceVehicule: 0.1,
    };
    expect(verifierPlaque(sansVehicule, '0987611616', 'avant')).toBe('PAS_UN_VEHICULE');
  });

  it('vue latérale → VUE_SANS_PLAQUE', () => {
    expect(verifierPlaque(vehiculeAvecPlaque(null), '0987611616', 'gauche')).toBe('VUE_SANS_PLAQUE');
  });

  it('immatriculation absente (null / vide) → NON_CONFORME, sans exception (parité Java)', () => {
    expect(verifierPlaque(vehiculeAvecPlaque('0987611616'), null, 'avant')).toBe('NON_CONFORME');
    expect(verifierPlaque(vehiculeAvecPlaque('0987611616'), '', 'avant')).toBe('NON_CONFORME');
  });
});

describe('recoHttp (mode réel) — confort : ne bloque jamais, lève en cas d’échec', () => {
  it('lève si le service répond non-2xx (→ le bandeau affiche « indisponible »)', async () => {
    const orig = globalThis.fetch;
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 503 } as Response);
    await expect(recoHttp.analyser(new Blob([new Uint8Array([1])]), 'avant')).rejects.toThrow();
    globalThis.fetch = orig;
  });

  it('lève si le réseau échoue (hors-ligne)', async () => {
    const orig = globalThis.fetch;
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('network down'));
    await expect(recoHttp.analyser(new Blob([new Uint8Array([1])]), 'avant')).rejects.toThrow();
    globalThis.fetch = orig;
  });
});

describe('construireResultat', () => {
  it('assemble lecture brute + statut ; jamais bloquant côté client', () => {
    const r = construireResultat(vehiculeAvecPlaque('0987611616'), '0987611616', 'avant');
    expect(r).toMatchObject({
      statut: 'CONFORME',
      plaqueLue: '0987611616',
      typeVehicule: 'voiture',
      estVehicule: true,
      bloquant: false,
    });
  });
});

describe('recoMock (adapter par défaut en dev)', () => {
  it('lit la plaque de démo sur la face avant', async () => {
    const r = await reco.analyser(new Blob([new Uint8Array([1, 2, 3])]), 'avant');
    expect(r.estVehicule).toBe(true);
    expect(r.plaque).toBe('0987611616');
  });

  it('ne lit pas de plaque sur une vue latérale', async () => {
    const r = await reco.analyser(new Blob([new Uint8Array([1])]), 'gauche');
    expect(r.plaque).toBeNull();
  });
});
