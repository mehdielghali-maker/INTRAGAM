import { StatutVersement } from './types';

// Mappe le statut backend vers la classe de pastille de la maquette.
const CLASSE: Record<StatutVersement, string> = {
  BROUILLON: 'st-brouillon',
  DEPOSE: 'st-depose',
  EN_CONTROLE: 'st-controle',
  VALIDE: 'st-valide',
  REJETE: 'st-rejete',
};

export default function StatutVersementBadge({
  statut,
  libelle,
}: {
  statut: StatutVersement;
  libelle: string;
}) {
  return (
    <span className={`st ${CLASSE[statut]}`}>
      <span className="dt" />
      {libelle}
    </span>
  );
}
