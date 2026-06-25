import { useState } from 'react';
import { useAgence } from '../../app/AgencyContext';
import SuiviDemandesTab from './SuiviDemandesTab';
import NouvelleDemandeTab from './NouvelleDemandeTab';
import './cotation.css';

const ETAPES = [
  { n: '1 · Envoyée', src: "par l'agence" },
  { n: '2 · En cours', src: 'souscripteur · API BPM' },
  { n: '3 · À finaliser', src: 'devis dispo sur PROASSUR' },
  { n: '4 · Affaire gagnée', src: 'quittance imprimée · PROASSUR' },
];

export default function CotationPage() {
  const { contexte } = useAgence();
  const consolide = contexte?.consolideActif ?? false;
  const [onglet, setOnglet] = useState<'suivi' | 'nouvelle'>('suivi');

  return (
    <section>
      <h1 className="page-h">Demande de cotation</h1>
      <p className="page-sub">
        Demande routée vers le central. L'agence transmet ; la cotation est réalisée dans
        PROASSUR. Le poste suit le statut, sans jamais tarifer ni rapatrier le devis.
      </p>

      <div className="flow-strip">
        {ETAPES.map((e, i) => (
          <span key={e.n} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span className="flow-step">
              <span className="s">{e.n}</span>
              <span className="src">{e.src}</span>
            </span>
            {i < ETAPES.length - 1 && <span className="flow-arrow">→</span>}
          </span>
        ))}
      </div>

      <div className="tabs">
        <button
          className={`tab-btn ${onglet === 'suivi' ? 'active' : ''}`}
          onClick={() => setOnglet('suivi')}
        >
          Suivi des demandes
        </button>
        <button
          className={`tab-btn ${onglet === 'nouvelle' ? 'active' : ''}`}
          onClick={() => setOnglet('nouvelle')}
        >
          Nouvelle demande
        </button>
      </div>

      {onglet === 'suivi' ? (
        <SuiviDemandesTab onNouvelle={() => setOnglet('nouvelle')} />
      ) : consolide ? (
        <div className="form-card">
          <div className="conso-banner">
            Mode consolidé (vue d'ensemble) : créer une demande concerne une agence précise.
            Sélectionnez une agence dans le commutateur en haut à droite.
          </div>
        </div>
      ) : (
        <NouvelleDemandeTab onEnvoye={() => setOnglet('suivi')} />
      )}
    </section>
  );
}
