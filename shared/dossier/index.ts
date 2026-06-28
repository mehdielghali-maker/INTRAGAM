// Socle « dossier en cours » (alias @dossier) : machine à états brouillon/validation UNIFIÉE
// (déclaration + souscription) + mécanisme d'auto-enregistrement. Modèle : présentiel par défaut,
// lien client en exception, rattachement PROASSUR/DECSIN UNIQUEMENT à la validation.

export type { StatutDossier, OrigineDossier } from './etats';
export {
  LIBELLES_DOSSIER,
  TRANSITIONS,
  peutTransitionner,
  estModifiable,
  estVerrouille,
  peutValider,
  estRattachable,
} from './etats';

export type { BrouillonStore, AutoEnregistrement } from './brouillon';
export { creerAutoEnregistrement } from './brouillon';
