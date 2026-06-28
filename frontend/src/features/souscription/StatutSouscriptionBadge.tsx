import { LIBELLES_STATUT, StatutSouscription } from '@souscription';

// Réutilise les pastilles de statut de la feature sinistre (mêmes couleurs GAM).
const CLASSE: Record<StatutSouscription, string> = {
  BROUILLON: 'st-brouillon',
  LIEN_ENVOYE: 'st-lien',
  A_VALIDER: 'st-avalider',
  RELANCE: 'st-relance',
  VALIDEE: 'st-validee',
};

export default function StatutSouscriptionBadge({ statut }: { statut: StatutSouscription }) {
  return (
    <span className={`st ${CLASSE[statut]}`}>
      <span className="dt" />
      {LIBELLES_STATUT[statut] ?? statut}
    </span>
  );
}
