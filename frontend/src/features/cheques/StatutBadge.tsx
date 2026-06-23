import { LIBELLE_STATUT, StatutCheque } from '../../api/cheques';

/** Pastille colorée par statut. Vert = encaissé, rouge = retourné, gris = en cours. */
export default function StatutBadge({ statut }: { statut: StatutCheque }) {
  const variante =
    statut === 'ENCAISSE' ? 'ok' : statut === 'RETOURNE' ? 'ko' : 'en-cours';
  return <span className={`badge badge-${variante}`}>{LIBELLE_STATUT[statut]}</span>;
}
