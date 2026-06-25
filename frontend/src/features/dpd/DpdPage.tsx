import { useState } from 'react';
import { useAgence } from '../../app/AgencyContext';
import SuiviDpdTab from './SuiviDpdTab';
import NouvelleDpdTab from './NouvelleDpdTab';
import MiseAJourDpdTab from './MiseAJourDpdTab';
import './dpd.css';

function BanniereConsolide() {
  return (
    <div className="form-card">
      <div className="conso-banner">
        Mode consolidé (vue d'ensemble) : créer une demande concerne une agence précise.
        Sélectionnez une agence dans le commutateur en haut à droite.
      </div>
    </div>
  );
}

const ETAPES = [
  { n: '1 · Envoyée', src: "par l'agence", handoff: false },
  { n: '2 · En validation', src: 'DR / central', handoff: false },
  { n: '3 · Accordée', src: 'échéancier validé', handoff: false },
  { n: '4 · Exécution', src: 'suivie dans « Suivi des échéanciers »', handoff: true },
];

type Onglet = 'suivi' | 'nouvelle' | 'maj';

export default function DpdPage() {
  const { contexte } = useAgence();
  const consolide = contexte?.consolideActif ?? false;
  const [onglet, setOnglet] = useState<Onglet>('suivi');

  return (
    <section>
      <h1 className="page-h">Accords d'échéancier — paiement différé</h1>
      <p className="page-sub">
        Demande routée vers le central/DR pour validation. L'échéancier est renseigné et validé
        dans PROASSUR ; le module transmet, suit le statut et reflète l'échéancier validé — sans
        jamais le saisir ni le recalculer.
      </p>

      <div className="flow-strip">
        {ETAPES.map((e, i) => (
          <span key={e.n} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span className={`flow-step ${e.handoff ? 'handoff' : ''}`}>
              <span className="s">{e.n}</span>
              <span className="src">{e.src}</span>
            </span>
            {i < ETAPES.length - 1 && <span className="flow-arrow">→</span>}
          </span>
        ))}
      </div>

      <div className="tabs">
        <button className={`tab-btn ${onglet === 'suivi' ? 'active' : ''}`} onClick={() => setOnglet('suivi')}>
          Suivi des demandes
        </button>
        <button className={`tab-btn ${onglet === 'nouvelle' ? 'active' : ''}`} onClick={() => setOnglet('nouvelle')}>
          Nouvelle demande
        </button>
        <button className={`tab-btn ${onglet === 'maj' ? 'active' : ''}`} onClick={() => setOnglet('maj')}>
          Mise à jour DPD
        </button>
      </div>

      {onglet === 'suivi' && <SuiviDpdTab onNouvelle={() => setOnglet('nouvelle')} />}
      {onglet === 'nouvelle' && (consolide ? <BanniereConsolide /> : <NouvelleDpdTab onEnvoye={() => setOnglet('suivi')} />)}
      {onglet === 'maj' && (consolide ? <BanniereConsolide /> : <MiseAJourDpdTab />)}
    </section>
  );
}
