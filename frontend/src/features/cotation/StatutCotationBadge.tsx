import { StatutCotation } from './types';

/** Badge coloré de statut, classe CSS dérivée de la clé d'enum (st-ENVOYEE, etc.). */
export default function StatutCotationBadge({
  statut,
  libelle,
}: {
  statut: StatutCotation;
  libelle: string;
}) {
  return (
    <span className={`st st-${statut}`}>
      <span className="dt" />
      {libelle}
    </span>
  );
}
