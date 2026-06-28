import { reco, construireResultat } from '@reco';
import type { AnalyseurPlaque } from '@sinistre-ui';

/**
 * Analyseur RECO de la PWA déclaration : appelle DIRECTEMENT le microservice RECO indépendant
 * (`@reco`, mock dev / http réel selon VITE_RECO_MODE) pour reconnaître le véhicule + lire la
 * plaque, puis compare à l'immatriculation du contrat CÔTÉ CLIENT. Aucun backend Java requis ;
 * en mode mock, fonctionne hors-ligne.
 */
export const analyseurReco: AnalyseurPlaque = async (photo, vue, immatriculation) => {
  const r = await reco.analyser(photo, vue);
  return construireResultat(r, immatriculation, vue);
};
