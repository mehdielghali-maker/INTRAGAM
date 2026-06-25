import { Icon } from '../../../app/icons';
import { CarteEcart, NiveauEcart } from '../types';
import { montantDA, ratioPct } from '../format';

/**
 * Carte « Écart à régulariser » = Encaissé − Versé en banque, en CUMULÉ. Le code couleur
 * et le message dépendent du ratio écart / CA des 12 derniers mois (4 niveaux) :
 * Correct (vert), Modéré (orange), Critique (rouge), Danger (noir).
 * (L'écart DU MOIS figure dans le bloc « Production & dépôts ».)
 */
const NIVEAUX: Record<NiveauEcart, { cls: string; message: string }> = {
  CORRECT: { cls: 'ec-correct', message: 'Correct' },
  MODERE: { cls: 'ec-modere', message: 'Modéré' },
  CRITIQUE: { cls: 'ec-critique', message: 'Critique' },
  DANGER: { cls: 'ec-danger', message: 'Danger' },
};

export default function KpiEcartCard({ ecart }: { ecart: CarteEcart }) {
  const n = NIVEAUX[ecart.niveau] ?? NIVEAUX.CORRECT;
  return (
    <div className={`card ${n.cls}`}>
      <div className="k-top">
        <span className="k-label">Écart à régulariser</span>
        <span className="k-ic">
          <Icon name="ecart" />
        </span>
      </div>
      <div className="k-val">{montantDA(ecart.valeur)}</div>
      <div className="k-sub">Écart / CA 12 mois · {ratioPct(ecart.pourcentage)}</div>
      <span className={`pill-state ${n.cls}`}>
        <span className="dot" />
        {n.message}
      </span>
    </div>
  );
}
