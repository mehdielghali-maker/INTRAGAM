import { VersementResponse } from './types';
import { montantDA, dateCourte } from './format';
import StatutVersementBadge from './StatutVersementBadge';

function PieceLien({ versement }: { versement: VersementResponse }) {
  if (versement.pieces.length === 0) {
    return <span className="act-txt">—</span>;
  }
  return (
    <span className="pj">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12" />
      </svg>
      Reçu
    </span>
  );
}

function dateDepot(v: VersementResponse): string {
  return dateCourte(v.dateDepot ?? v.dateCreation);
}

export default function SuiviVersementsTable({
  versements,
}: {
  versements: VersementResponse[];
}) {
  return (
    <>
      {/* Desktop : tableau */}
      <table className="list">
        <thead>
          <tr>
            <th style={{ width: '12%' }}>Date dépôt</th>
            <th style={{ width: '20%' }}>Agence</th>
            <th style={{ width: '13%' }}>Mois SF</th>
            <th style={{ width: '15%' }}>Montant versé</th>
            <th style={{ width: '13%' }}>Référence</th>
            <th style={{ width: '14%' }}>Statut</th>
            <th style={{ width: '13%' }}>Pièce</th>
          </tr>
        </thead>
        <tbody>
          {versements.map((v) => (
            <tr key={v.id}>
              <td>{dateDepot(v)}</td>
              <td>{v.codeAgence}</td>
              <td>{v.moisLibelle}</td>
              <td className="amt">{montantDA(v.montantVerse)}</td>
              <td>{v.referenceBordereau ?? v.reference ?? '—'}</td>
              <td>
                <StatutVersementBadge statut={v.statut} libelle={v.statutLibelle} />
              </td>
              <td>
                <PieceLien versement={v} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Mobile : cartes */}
      <div className="vers-cards">
        {versements.map((v) => (
          <div className="vers-card" key={v.id}>
            <div className="l1">
              <span className="ref">{v.referenceBordereau ?? v.reference ?? '—'}</span>
              <StatutVersementBadge statut={v.statut} libelle={v.statutLibelle} />
            </div>
            <div className="who">{montantDA(v.montantVerse)}</div>
            <div className="meta">
              {v.moisLibelle} · {v.codeAgence} · dépôt {dateDepot(v)}
            </div>
            <div className="l-act">
              <PieceLien versement={v} />
            </div>
          </div>
        ))}
      </div>

      {versements.length === 0 && (
        <p className="act-txt" style={{ marginTop: 16 }}>
          Aucun versement déposé.
        </p>
      )}
    </>
  );
}
