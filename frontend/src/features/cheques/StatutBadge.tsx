import { LIBELLE_STATUT, StatutCheque } from '../../api/cheques';

// Mappe le statut vers la classe de pastille (cohérent avec les autres sections).
const CLASSE: Record<StatutCheque, string> = {
  EMIS: 'st-emis',
  IMPRIME: 'st-imprime',
  REMIS_AGENCE: 'st-remis-agence',
  REMIS_BENEFICIAIRE: 'st-remis-beneficiaire',
  ENCAISSE: 'st-encaisse',
  RETOURNE: 'st-retourne',
};

/** Pastille de statut du chèque (style commun « .st »). */
export default function StatutBadge({ statut }: { statut: StatutCheque }) {
  return (
    <span className={`st ${CLASSE[statut]}`}>
      <span className="dt" />
      {LIBELLE_STATUT[statut]}
    </span>
  );
}
