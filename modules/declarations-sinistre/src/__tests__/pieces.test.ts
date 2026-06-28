import { describe, expect, it } from 'vitest';
import { peutEnvoyer, Piece, piecesManquantes, TypePiece, vuesVehicule } from '@decsin';

function piece(type: TypePiece): Piece {
  return { id: type, type, nom: `${type}.jpg`, estPdf: false, tailleKo: 100 };
}

const OBLIGATOIRES: TypePiece[] = ['recto_constat', 'verso_constat', 'face_avant', 'face_arriere', 'cote_gauche', 'cote_droit'];

describe('règle de validation des pièces', () => {
  it('bloque tant que le constat (recto/verso) et les 4 faces ne sont pas fournis', () => {
    const d = { pieces: [] as Piece[] };
    expect(peutEnvoyer(d)).toBe(false);
    expect(piecesManquantes(d)).toEqual(OBLIGATOIRES);
  });

  it('autorise l\'envoi avec recto/verso + 4 faces (sans tiers)', () => {
    const d = { pieces: OBLIGATOIRES.map(piece) };
    expect(peutEnvoyer(d)).toBe(true);
    expect(piecesManquantes(d)).toEqual([]);
  });

  it('exige l\'assurance adverse si un tiers est déclaré', () => {
    const base = { pieces: OBLIGATOIRES.map(piece), compagnieAdverse: 'CAAR' };
    expect(peutEnvoyer(base)).toBe(false);
    expect(piecesManquantes(base)).toContain('assurance_adverse');

    const avecAdverse = { ...base, pieces: [...base.pieces, piece('assurance_adverse')] };
    expect(peutEnvoyer(avecAdverse)).toBe(true);
  });

  it('n\'exige pas l\'assurance adverse sans tiers', () => {
    const d = { pieces: OBLIGATOIRES.map(piece) };
    expect(piecesManquantes(d)).not.toContain('assurance_adverse');
  });

  it('considère un tiers dès que le véhicule adverse est renseigné', () => {
    const d = { pieces: OBLIGATOIRES.map(piece), vehiculeAdverse: '55512-110-35' };
    expect(piecesManquantes(d)).toEqual(['assurance_adverse']);
  });

  it('propose 5 vues véhicule, dont le toit qui est FACULTATIF (non bloquant)', () => {
    const vues = vuesVehicule();
    expect(vues.map((v) => v.type)).toEqual(['face_avant', 'face_arriere', 'cote_gauche', 'cote_droit', 'face_toit']);
    expect(vues.find((v) => v.type === 'face_toit')?.obligatoire).toBe(false);
    expect(vues.find((v) => v.type === 'face_toit')?.masque).toBe(true);
    // Constat + 4 faces présents, sans toit → envoi autorisé.
    expect(peutEnvoyer({ pieces: OBLIGATOIRES.map(piece) })).toBe(true);
    expect(piecesManquantes({ pieces: OBLIGATOIRES.map(piece) })).not.toContain('face_toit');
  });
});
