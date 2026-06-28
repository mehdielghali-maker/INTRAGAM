import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { brouillonsLocaux, decsin, Declaration, LIBELLES_STATUT, StatutDeclaration } from '@decsin';
import StatutDeclarationBadge from './StatutDeclarationBadge';

const CHECK = 'M5 12l5 5L20 6';
const RELANCE = 'M21 12a9 9 0 1 1-3-6.7M21 4v5h-5';
const PEN = 'M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z';

type Filtre = 'TOUS' | StatutDeclaration;
const FILTRES: Filtre[] = ['TOUS', 'BROUILLON', 'LIEN_ENVOYE', 'A_VALIDER', 'RELANCE', 'VALIDEE'];

/**
 * Onglet « Suivi des déclarations » : liste les brouillons présentiel (locaux) ET les déclarations
 * envoyées (DECSIN), avec filtres par statut. Un brouillon se « Reprend » (édition) ; « À valider »
 * se contrôle (→ PROASSUR) ou se renvoie au client (RELANCE).
 */
export default function SuiviDeclarationsTab() {
  const navigate = useNavigate();
  const [declarations, setDeclarations] = useState<Declaration[]>([]);
  const [filtre, setFiltre] = useState<Filtre>('TOUS');
  const [message, setMessage] = useState<string | null>(null);

  const reprendre = (d: Declaration) => navigate(`/declaration-sinistre?reprendre=${encodeURIComponent(d.idLocal)}`);
  const ouvrir = (d: Declaration) =>
    d.statut === 'BROUILLON' ? reprendre(d) : navigate(`/declaration-sinistre/${d.idLocal}`);

  const recharger = useCallback(async () => {
    // Brouillons présentiel (locaux, jamais envoyés) + déclarations DECSIN (envoyées/validées).
    const [brouillons, envoyees] = await Promise.all([brouillonsLocaux.lister(), decsin.getDeclarations()]);
    setDeclarations([...brouillons, ...envoyees]);
  }, []);

  useEffect(() => {
    void recharger();
  }, [recharger]);

  const liste = filtre === 'TOUS' ? declarations : declarations.filter((d) => d.statut === filtre);

  async function renvoyerAuClient(d: Declaration) {
    await decsin.creerDeclaration({ ...d, statut: 'RELANCE' });
    setMessage(`Déclaration ${d.code} renvoyée au client pour correction.`);
    await recharger();
  }

  function relancer(d: Declaration) {
    setMessage(`Relance envoyée au client pour la déclaration ${d.code}.`);
  }

  function actions(d: Declaration) {
    const stop = (fn: () => void) => (e: React.MouseEvent) => { e.stopPropagation(); fn(); };
    if (d.statut === 'BROUILLON') {
      return (
        <button className="act-btn act-valider" onClick={stop(() => reprendre(d))}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={PEN} /></svg>
          Reprendre
        </button>
      );
    }
    if (d.statut === 'A_VALIDER') {
      return (
        <>
          <button className="act-btn act-valider" onClick={stop(() => ouvrir(d))}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.2}><path d={CHECK} /></svg>
            Contrôler
          </button>{' '}
          <button className="act-btn act-sec" onClick={stop(() => renvoyerAuClient(d))}>Renvoyer</button>
        </>
      );
    }
    if (d.statut === 'LIEN_ENVOYE' || d.statut === 'RELANCE') {
      return (
        <button className="act-btn act-sec" onClick={stop(() => relancer(d))}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={RELANCE} /></svg>
          Relancer le client
        </button>
      );
    }
    return d.numSinistre ? <span className="num-sin">N° {d.numSinistre}</span> : <span className="hint">—</span>;
  }

  return (
    <div>
      {message && <p style={{ color: 'var(--vert)', fontWeight: 600, marginBottom: 12 }}>{message}</p>}

      <div className="filters">
        {FILTRES.map((f) => (
          <button key={f} className={`fchip ${filtre === f ? 'active' : ''}`} onClick={() => setFiltre(f)}>
            {f === 'TOUS' ? 'Toutes' : LIBELLES_STATUT[f]}
          </button>
        ))}
      </div>

      {/* Table (desktop) */}
      <table className="list">
        <thead>
          <tr>
            <th>Code</th>
            <th>Véhicule</th>
            <th>Conducteur / client</th>
            <th>Date sinistre</th>
            <th>Origine</th>
            <th>Statut</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {liste.map((d) => (
            <tr key={d.idLocal} className="ouvrable" onClick={() => ouvrir(d)}>
              <td><b>{d.code}</b></td>
              <td>{d.marque}<br /><span className="hint">{d.immatriculation} · {d.numPolice}</span></td>
              <td>{d.client?.nom ?? d.conducteur ?? '—'}</td>
              <td>{d.dateSinistre || '—'}</td>
              <td>{d.origine === 'AGA' ? 'AGA' : 'Client'}</td>
              <td><StatutDeclarationBadge statut={d.statut} /></td>
              <td>{actions(d)}</td>
            </tr>
          ))}
          {liste.length === 0 && (
            <tr><td colSpan={7} className="vide">Aucune déclaration.</td></tr>
          )}
        </tbody>
      </table>

      {/* Cartes (mobile) */}
      <div className="cards">
        {liste.map((d) => (
          <div key={d.idLocal} className="dc-card ouvrable" onClick={() => ouvrir(d)}>
            <div className="dc-h">
              <span className="dc-code">{d.code}</span>
              <StatutDeclarationBadge statut={d.statut} />
            </div>
            <div className="dc-row"><span className="k">Véhicule :</span> {d.marque} · {d.immatriculation}</div>
            <div className="dc-row"><span className="k">Client :</span> {d.client?.nom ?? d.conducteur ?? '—'}</div>
            <div className="dc-row"><span className="k">Date :</span> {d.dateSinistre || '—'} · {d.origine === 'AGA' ? 'AGA' : 'Client'}</div>
            <div className="dc-actions">{actions(d)}</div>
          </div>
        ))}
        {liste.length === 0 && <p className="vide">Aucune déclaration.</p>}
      </div>
    </div>
  );
}
