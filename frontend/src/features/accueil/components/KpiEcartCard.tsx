import { Icon } from '../../../app/icons';
import { CarteEcart } from '../types';
import { montantDA, ratioPct } from '../format';

/**
 * Carte d'alerte « Écart à régulariser » = Encaissé − Versé en banque. Devient terracotta
 * (classe {@code alert}) si l'écart dépasse le seuil de config, verte sinon.
 */
export default function KpiEcartCard({ ecart }: { ecart: CarteEcart }) {
  return (
    <div className={`card ${ecart.aRegulariser ? 'alert' : ''}`}>
      <div className="k-top">
        <span className="k-label">Écart à régulariser</span>
        <span className="k-ic">
          <Icon name="ecart" />
        </span>
      </div>
      <div className="k-val">{montantDA(ecart.valeur)}</div>
      <div className="k-sub">Encaissé − versé · {ratioPct(ecart.pourcentage)}</div>
      <span className={`pill-state ${ecart.aRegulariser ? '' : 'ok'}`}>
        <span className="dot" />
        {ecart.aRegulariser ? 'À régulariser' : 'Conforme'}
      </span>
    </div>
  );
}
