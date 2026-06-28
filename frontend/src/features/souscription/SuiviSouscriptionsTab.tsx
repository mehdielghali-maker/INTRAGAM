import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LIBELLES_STATUT, souscription, Souscription, StatutSouscription } from '@souscription';
import StatutSouscriptionBadge from './StatutSouscriptionBadge';

type Filtre = 'TOUS' | StatutSouscription;
const FILTRES: Filtre[] = ['TOUS', 'BROUILLON', 'ENREGISTREE', 'INCOMPLETE'];

/** Onglet « Suivi des souscriptions » : liste, filtres, ouverture du détail. */
export default function SuiviSouscriptionsTab() {
  const navigate = useNavigate();
  const [liste, setListe] = useState<Souscription[]>([]);
  const [filtre, setFiltre] = useState<Filtre>('TOUS');

  const recharger = useCallback(async () => {
    setListe(await souscription.getSouscriptions());
  }, []);

  useEffect(() => {
    void recharger();
  }, [recharger]);

  const affichees = filtre === 'TOUS' ? liste : liste.filter((s) => s.statut === filtre);
  const ouvrir = (s: Souscription) => navigate(`/souscription-auto/${s.idLocal}`);

  return (
    <div>
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
              <td><button className="act-btn act-sec" onClick={(e) => { e.stopPropagation(); ouvrir(s); }}>Ouvrir</button></td>
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
          </div>
        ))}
        {affichees.length === 0 && <p className="vide">Aucune souscription.</p>}
      </div>
    </div>
  );
}
