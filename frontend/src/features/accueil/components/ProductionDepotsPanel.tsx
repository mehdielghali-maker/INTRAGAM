import { ProductionDepots } from '../types';
import { montantDA } from '../format';

/**
 * Bloc « Production & dépôts du mois » : du chiffre émis à l'argent en banque.
 * Barres comparées (production = 100 % de référence) + encart écart, cohérent avec le KPI 3.
 */
export default function ProductionDepotsPanel({ data }: { data: ProductionDepots }) {
  const pct = (valeur: number) =>
    data.production > 0 ? Math.min(100, (valeur / data.production) * 100) : 0;

  const barres = [
    { nom: 'Production émise', montant: data.production, largeur: 100, couleur: 'var(--gris)' },
    { nom: 'Encaissé', montant: data.encaisse, largeur: pct(data.encaisse), couleur: 'var(--vert)' },
    { nom: 'Déposé en banque', montant: data.depose, largeur: pct(data.depose), couleur: 'var(--or)' },
  ];

  return (
    <div className="panel">
      <div className="panel-h">Production &amp; dépôts du mois</div>
      <div className="panel-sub">Du chiffre émis à l'argent en banque</div>

      {barres.map((b) => (
        <div className="bar-row" key={b.nom}>
          <div className="bar-top">
            <span className="bar-name">{b.nom}</span>
            <span className="bar-amt">{montantDA(b.montant)}</span>
          </div>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${b.largeur}%`, background: b.couleur }} />
          </div>
        </div>
      ))}

      <div className="ecart-box">
        <span className="lab">Écart à régulariser (encaissé − versé)</span>
        <span className="v">{montantDA(data.ecart)}</span>
      </div>
    </div>
  );
}
