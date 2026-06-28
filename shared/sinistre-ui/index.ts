// Socle de capture PARTAGÉ (React), piloté par un CATALOGUE — réutilisé par la déclaration
// (poste + client) et la souscription (poste + agent). Aucune dépendance à un domaine précis.
export { default as PiecesCapture } from './PiecesCapture';
export { default as CaptureVehicule } from './CaptureVehicule';
export { default as CaptureDocuments } from './CaptureDocuments';
export { default as ApercusControle } from './ApercusControle';
export { default as VerificationPlaque } from './VerificationPlaque';
export { default as Lightbox } from './Lightbox';
export { SILHOUETTES, ICONES_VUE } from './silhouettes';
export { preparerPiece, identifiant } from './media';
export type { PiecePreparee } from './media';
export {
  piecesDuGroupe,
  vuesVehicule,
  definition,
} from './catalogue';
export type { Catalogue, DefinitionPiece, GroupeDef, PieceCapturee } from './catalogue';
