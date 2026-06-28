// Catalogue de capture du domaine SOUSCRIPTION AUTO (format générique du socle @sinistre-ui).
// Pièces issues de l'app GAM Expert : identité (CNI), permis, carte grise, photos véhicule (8 vues),
// documents contrat. Règle : CNI recto/verso + permis (recto) + carte grise + 4 faces véhicule.

import type { Catalogue, DefinitionPiece } from '@sinistre-ui/catalogue';
import { ICONES_VUE, SILHOUETTES } from '@sinistre-ui/silhouettes';
import { TypePieceSouscription } from './types';

const DEFS: (DefinitionPiece & { type: TypePieceSouscription })[] = [
  // Identité & permis
  { type: 'cni_recto', libelle: 'CNI (recto)', sousTitre: "pièce d'identité", groupe: 'identite', obligatoire: true, masque: false },
  { type: 'cni_verso', libelle: 'CNI (verso)', sousTitre: "pièce d'identité", groupe: 'identite', obligatoire: true, masque: false },
  { type: 'permis_recto', libelle: 'Permis (recto)', sousTitre: 'permis de conduire', groupe: 'identite', obligatoire: true, masque: false },
  { type: 'permis_verso', libelle: 'Permis (verso)', sousTitre: 'facultatif', groupe: 'identite', obligatoire: false, masque: false },

  // Documents
  { type: 'carte_grise', libelle: 'Carte grise', sousTitre: 'certificat immatriculation', groupe: 'documents', obligatoire: true, masque: false },
  { type: 'attestation', libelle: "Attestation d'assurance", sousTitre: 'facultatif', groupe: 'documents', obligatoire: false, masque: false },
  { type: 'contrat_signe', libelle: 'Contrat signé', sousTitre: 'facultatif', groupe: 'documents', obligatoire: false, masque: false },
  { type: 'autre', libelle: 'Autre document', sousTitre: 'facultatif', groupe: 'documents', obligatoire: false, masque: false },

  // Photos véhicule (sélecteur de vues) — 4 faces obligatoires, le reste facultatif
  { type: 'veh_avant', libelle: 'Avant', sousTitre: 'avec guide', groupe: 'photos_vehicule', obligatoire: true, masque: true },
  { type: 'veh_arriere', libelle: 'Arrière', sousTitre: 'avec guide', groupe: 'photos_vehicule', obligatoire: true, masque: true },
  { type: 'veh_gauche', libelle: 'Côté gauche', sousTitre: 'avec guide', groupe: 'photos_vehicule', obligatoire: true, masque: true },
  { type: 'veh_droit', libelle: 'Côté droit', sousTitre: 'avec guide', groupe: 'photos_vehicule', obligatoire: true, masque: true },
  { type: 'veh_av_g', libelle: 'Avant-gauche', sousTitre: 'facultatif', groupe: 'photos_vehicule', obligatoire: false, masque: false },
  { type: 'veh_av_d', libelle: 'Avant-droit', sousTitre: 'facultatif', groupe: 'photos_vehicule', obligatoire: false, masque: false },
  { type: 'veh_ar_g', libelle: 'Arrière-gauche', sousTitre: 'facultatif', groupe: 'photos_vehicule', obligatoire: false, masque: false },
  { type: 'veh_ar_d', libelle: 'Arrière-droit', sousTitre: 'facultatif', groupe: 'photos_vehicule', obligatoire: false, masque: false },
  { type: 'veh_vin', libelle: 'N° de châssis', sousTitre: 'facultatif', groupe: 'photos_vehicule', obligatoire: false, masque: false },
  { type: 'veh_interieur', libelle: 'Intérieur', sousTitre: 'facultatif', groupe: 'photos_vehicule', obligatoire: false, masque: false },
];

/** Types obligatoires pour valider une souscription auto. */
export const REQUIS: TypePieceSouscription[] = [
  'cni_recto', 'cni_verso', 'permis_recto', 'carte_grise', 'veh_avant', 'veh_arriere', 'veh_gauche', 'veh_droit',
];

export function piecesManquantes(pieces: { type: string }[]): TypePieceSouscription[] {
  const present = new Set(pieces.map((p) => p.type));
  return REQUIS.filter((t) => !present.has(t));
}

export function peutEnvoyer(pieces: { type: string }[]): boolean {
  return piecesManquantes(pieces).length === 0;
}

/** Catalogue souscription auto, consommé par le socle de capture. */
export const catalogueSouscription: Catalogue = {
  groupes: [
    { id: 'identite', libelle: 'Identité & permis' },
    { id: 'documents', libelle: 'Documents' },
    { id: 'photos_vehicule', libelle: 'Photos du véhicule', hint: '· 4 faces obligatoires · vues complémentaires facultatives' },
  ],
  definitions: DEFS,
  groupeVehicule: 'photos_vehicule',
  // Silhouettes des 4 faces réutilisées du socle ; diagonales / VIN / intérieur sans silhouette (cadre seul).
  silhouettes: {
    veh_avant: SILHOUETTES.face_avant,
    veh_arriere: SILHOUETTES.face_arriere,
    veh_gauche: SILHOUETTES.cote_gauche,
    veh_droit: SILHOUETTES.cote_droit,
  },
  iconesVue: {
    veh_avant: ICONES_VUE.face_avant,
    veh_arriere: ICONES_VUE.face_arriere,
    veh_gauche: ICONES_VUE.cote_gauche,
    veh_droit: ICONES_VUE.cote_droit,
    veh_av_g: ICONES_VUE.cote_gauche,
    veh_av_d: ICONES_VUE.cote_droit,
    veh_ar_g: ICONES_VUE.cote_gauche,
    veh_ar_d: ICONES_VUE.cote_droit,
  },
  estObligatoire: (type) => DEFS.find((d) => d.type === type)?.obligatoire ?? false,
  piecesManquantes: (pieces) => piecesManquantes(pieces),
};
