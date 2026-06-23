import { Icon, IconName } from '../../../app/icons';
import { Variation } from '../types';
import { variationLabel } from '../format';

/**
 * Carte KPI générique : libellé + icône, grande valeur, puce de variation (couleur
 * SÉMANTIQUE : vert = favorable, terra = défavorable) et ligne de référence.
 */
export default function KpiCard({
  icon,
  label,
  valeur,
  variation,
  sousLigne,
}: {
  icon: IconName;
  label: string;
  valeur: string;
  variation: Variation;
  sousLigne: string;
}) {
  return (
    <div className="card">
      <div className="k-top">
        <span className="k-label">{label}</span>
        <span className="k-ic">
          <Icon name={icon} />
        </span>
      </div>
      <div className="k-val">{valeur}</div>
      <span className={`delta ${variation.favorable ? 'good' : 'bad'}`}>
        <Icon name={variation.valeur >= 0 ? 'fleche-haut' : 'fleche-bas'} />
        {variationLabel(variation)}
      </span>
      <div className="delta-ref">{sousLigne}</div>
    </div>
  );
}
