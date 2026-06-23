import { Periode } from '../features/accueil/types';

function initiales(nom: string): string {
  const mots = nom.replace(/^Agence\s*/i, '').trim().split(/\s+/);
  return mots.slice(0, 2).map((m) => m.charAt(0)).join('').toUpperCase() || 'GA';
}

/**
 * Barre supérieure : logo GAM (tuile blanche) + identité agence à gauche ; sélecteur
 * de période fonctionnel + avatar à droite.
 */
export default function Topbar({
  agence,
  moisAnnee,
  periode,
  onPeriodeChange,
}: {
  agence: { nom: string; code: string } | null;
  moisAnnee: string;
  periode: Periode;
  onPeriodeChange: (p: Periode) => void;
}) {
  const nom = agence?.nom ?? 'Agence';
  const code = agence?.code ?? '—';
  return (
    <header className="topbar">
      <div className="brandblock">
        <span className="logo-tile">
          <img src="/logo-gam.webp" alt="GAM Assurances" />
        </span>
        <div>
          <div className="agence-nom">
            {nom}
            <span className="maquette-tag">Données fictives</span>
          </div>
          <div className="agence-meta">Code {code} · Espace agence · session ouverte</div>
        </div>
      </div>

      <div className="ca-zone">
        {moisAnnee && <span className="period-chip">Période · {moisAnnee}</span>}
        <select
          className="period-select"
          value={periode}
          aria-label="Période"
          onChange={(e) => onPeriodeChange(e.target.value as Periode)}
        >
          <option value="YTD">YTD</option>
          <option value="MOIS_COURANT">Mois courant</option>
        </select>
        <div className="uavatar" title={nom}>
          {initiales(nom)}
        </div>
      </div>
    </header>
  );
}
