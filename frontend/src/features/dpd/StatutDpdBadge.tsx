import { StatutDpd } from './types';

export default function StatutDpdBadge({ statut, libelle }: { statut: StatutDpd; libelle: string }) {
  return (
    <span className={`st st-${statut}`}>
      <span className="dt" />
      {libelle}
    </span>
  );
}
