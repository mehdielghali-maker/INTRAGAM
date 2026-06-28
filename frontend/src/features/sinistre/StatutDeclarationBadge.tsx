import { LIBELLES_STATUT, StatutDeclaration } from '@decsin';

const CLASSE: Record<StatutDeclaration, string> = {
  LIEN_ENVOYE: 'st-lien',
  A_VALIDER: 'st-avalider',
  VALIDEE: 'st-validee',
  INCOMPLETE: 'st-incomplete',
};

/** Pastille de statut d'une déclaration (mêmes couleurs que la maquette). */
export default function StatutDeclarationBadge({ statut }: { statut: StatutDeclaration }) {
  return (
    <span className={`st ${CLASSE[statut]}`}>
      <span className="dt" />
      {LIBELLES_STATUT[statut] ?? statut}
    </span>
  );
}
