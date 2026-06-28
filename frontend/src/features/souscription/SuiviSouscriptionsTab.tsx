import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { brouillonsLocaux, LIBELLES_STATUT, souscription, Souscription, StatutSouscription } from '@souscription';
import StatutSouscriptionBadge from './StatutSouscriptionBadge';

const PEN = 'M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z';
const RELANCE = 'M21 12a9 9 0 1 1-3-6.7M21 4v5h-5';

type Filtre = 'TOUS' | StatutSouscription;
const FILTRES: Filtre[] = ['TOUS', 'BROUILLON', 'LIEN_ENVOYE', 'A_VALIDER', 'RELANCE', 'VALIDEE'];

/**
 * Onglet « Suivi des souscriptions » : brouillons présentiel (locaux) + souscriptions envoyées (GAM),
 * filtres par statut. Un brouillon se « Reprend » ; « À valider » se contrôle ou se renvoie (RELANCE).
 */
export default function SuiviSouscriptionsTab() {
  const navigate = useNavigate();
  const [liste, setListe] = useState<Souscription[]>([]);
  const [filtre, setFiltre] = useState<Filtre>('TOUS');
  const [message, setMessage] = useState<string | null>(null);

  const recharger = useCallback(async () => {
    const [brouillons, envoyees] = await Promise.all([brouillonsLocaux.lister(), souscription.getSouscriptions()]);
    setListe([...brouillons, ...envoyees]);
  }, []);

  useEffect(() => {
    void recharger();
  }, [recharger]);

  const affichees = filtre === 'TOUS' ? liste : liste.filter((s) => s.statut === filtre);
  const reprendre = (s: Souscription) => navigate(`/souscription-auto?reprendre=${encodeURIComponent(s.idLocal)}`);
  const ouvrir = (s: Souscription) =>
    s.statut === 'BROUILLON' ? reprendre(s) : navigate(`/souscription-auto/${s.idLocal}`);

  async function renvoyerAuClient(s: Souscription) {
    await souscription.creerSouscription({ ...s, statut: 'RELANCE' });
    setMessage(`Souscription ${s.reference} renvoyée au client pour correction.`);
    await recharger();
  }

  function actions(s: Souscription) {
    const stop = (fn: () => void) => (e: React.MouseEvent) => { e.stopPropagation(); fn(); };
    if (s.statut === 'BROUILLON') {
      return (
        <button className="act-btn act-valider" onClick={stop(() => reprendre(s))}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={PEN} /></svg>
          Reprendre
        </button>
      );
    }
    if (s.statut === 'A_VALIDER') {
      return (
        <>
          <button className="act-btn act-valider" onClick={stop(() => ouvrir(s))}>Contrôler</button>{' '}
          <button className="act-btn act-sec" onClick={stop(() => renvoyerAuClient(s))}>Renvoyer</button>
        </>
      );
    }
    if (s.statut === 'LIEN_ENVOYE' || s.statut === 'RELANCE') {
      return (
        <button className="act-btn act-sec" onClick={stop(() => setMessage(`Relance envoyée pour ${s.reference}.`))}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={RELANCE} /></svg>
          Relancer le client
        </button>
      );
    }
    return <button className="act-btn act-sec" onClick={stop(() => ouvrir(s))}>Ouvrir</button>;
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

      <table className="list">
        <thead>
          <tr><th>Référence</th><th>Police</th><th>Client</th><th>Véhicule</th><th>Statut</th><th>Action</th></tr>
        </thead>
        <tbody>
          {affichees.map((s) => (
            <tr key={s.idLocal} className="ouvrable" onClick={() => ouvrir(s)}>
              <td><b>{s.reference}</b></td>
              <td>{s.numeroPolice}</td>
              <td>{s.nomClient}</td>
              <td>{s.vehicule.marque ?? '—'} {s.vehicule.immatriculation ? `· ${s.vehicule.immatriculation}` : ''}</td>
              <td><StatutSouscriptionBadge statut={s.statut} /></td>
              <td>{actions(s)}</td>
            </tr>
          ))}
          {affichees.length === 0 && <tr><td colSpan={6} className="vide">Aucune souscription.</td></tr>}
        </tbody>
      </table>

      <div className="cards">
        {affichees.map((s) => (
          <div key={s.idLocal} className="dc-card ouvrable" onClick={() => ouvrir(s)}>
            <div className="dc-h"><span className="dc-code">{s.reference}</span><StatutSouscriptionBadge statut={s.statut} /></div>
            <div className="dc-row"><span className="k">Police :</span> {s.numeroPolice}</div>
            <div className="dc-row"><span className="k">Client :</span> {s.nomClient}</div>
            <div className="dc-row"><span className="k">Véhicule :</span> {s.vehicule.marque ?? '—'} {s.vehicule.immatriculation ?? ''}</div>
            <div className="dc-actions">{actions(s)}</div>
          </div>
        ))}
        {affichees.length === 0 && <p className="vide">Aucune souscription.</p>}
      </div>
    </div>
  );
}
