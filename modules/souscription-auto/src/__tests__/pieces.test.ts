import { describe, expect, it } from 'vitest';
import { catalogueSouscription, peutEnvoyer, piecesManquantes, REQUIS } from '@souscription';

const piece = (type: string) => ({ type });
const present = REQUIS.map((t) => piece(t));

describe('règle de validation — souscription auto', () => {
  it('bloque tant que CNI r/v + permis + carte grise + 4 faces ne sont pas fournis', () => {
    expect(peutEnvoyer([])).toBe(false);
    expect(piecesManquantes([])).toEqual(REQUIS);
    expect(REQUIS).toContain('carte_grise');
    expect(REQUIS).toContain('veh_avant');
  });

  it('autorise l\'enregistrement quand toutes les pièces obligatoires sont présentes', () => {
    expect(peutEnvoyer(present)).toBe(true);
    expect(piecesManquantes(present)).toEqual([]);
  });

  it('les vues complémentaires et le permis verso sont facultatifs', () => {
    expect(REQUIS).not.toContain('veh_av_g');
    expect(REQUIS).not.toContain('veh_vin');
    expect(REQUIS).not.toContain('permis_verso');
    expect(REQUIS).not.toContain('attestation');
  });

  it('le catalogue expose un groupe de vues véhicule avec silhouettes des 4 faces', () => {
    expect(catalogueSouscription.groupeVehicule).toBe('photos_vehicule');
    const vues = catalogueSouscription.definitions.filter((d) => d.groupe === 'photos_vehicule');
    expect(vues.length).toBeGreaterThanOrEqual(8);
    for (const face of ['veh_avant', 'veh_arriere', 'veh_gauche', 'veh_droit']) {
      expect(catalogueSouscription.silhouettes[face]).toBeTruthy();
    }
  });
});
