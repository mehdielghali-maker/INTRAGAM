// Catalogue de capture du domaine DÉCLARATION, au format générique du socle (@sinistre-ui).
// Bâti depuis les pièces/règles existantes (pieces.ts) — comportement identique (5 vues, tiers).

import type { Catalogue } from '@sinistre-ui/catalogue';
import { ICONES_VUE, SILHOUETTES } from '@sinistre-ui/silhouettes';
import { GROUPES, PIECES } from './pieces';

/** Contexte de validation de la déclaration : un tiers est-il déclaré (assurance adverse requise). */
export interface ContexteDeclaration {
  tiers: boolean;
}

export const catalogueDeclaration: Catalogue = {
  groupes: GROUPES.map((g) => ({
    id: g.id,
    libelle: g.libelle,
    hint: g.id === 'vehicule' ? '· 4 faces obligatoires + toit (facultatif) · cadrez chaque vue' : undefined,
  })),
  definitions: PIECES.map((p) => ({
    type: p.type,
    libelle: p.libelle,
    sousTitre: p.sousTitre,
    groupe: p.groupe,
    obligatoire: p.obligatoire,
    masque: p.masque,
  })),
  // Clés des silhouettes/icônes = types de vues de la déclaration (face_avant, cote_gauche, …).
  silhouettes: SILHOUETTES,
  iconesVue: ICONES_VUE,
  groupeVehicule: 'vehicule',
  estObligatoire: (type, ctx) => {
    const def = PIECES.find((p) => p.type === type);
    return Boolean(def?.obligatoire) || (type === 'assurance_adverse' && ctx.tiers);
  },
  piecesManquantes: (pieces, ctx) => {
    const present = new Set(pieces.map((p) => p.type));
    const requis: string[] = PIECES.filter((p) => p.obligatoire).map((p) => p.type);
    if (ctx.tiers) {
      requis.push('assurance_adverse');
    }
    return requis.filter((t) => !present.has(t));
  },
};
