import { useState } from 'react';
import NouvelleSouscriptionTab from './NouvelleSouscriptionTab';
import SuiviSouscriptionsTab from './SuiviSouscriptionsTab';
import '../sinistre/sinistre.css';

const PLUS = 'M12 5v14M5 12h14';
const LIST = 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01';

type Onglet = 'suivi' | 'nouvelle';

/**
 * Souscription auto — face AGA. Frise du flux (Police → Pièces → Contrôle → Enregistrée) +
 * onglets « Suivi » et « Nouvelle souscription ». Réutilise le design de la feature sinistre.
 */
export default function SouscriptionPage() {
  const [onglet, setOnglet] = useState<Onglet>('suivi');

  return (
    <section className="sinistre">
      <div className="page-h">Souscription auto</div>
      <p className="page-sub">
        Recherchez une police, capturez les pièces (CNI, permis, carte grise, photos du véhicule)
        puis contrôlez la complétude avant d'enregistrer la souscription.
      </p>

      <div className="flow-strip">
        <div className="flow-step"><span className="s">1 · Police</span><span className="src">recherche & sélection</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">2 · Pièces</span><span className="src">CNI · permis · carte grise · photos</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">3 · Contrôle</span><span className="src">complétude</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">4 · Enregistrée</span><span className="src">transmise à GAM</span></div>
      </div>

      <div className="tabs">
        <button className={`tab-btn ${onglet === 'suivi' ? 'active' : ''}`} onClick={() => setOnglet('suivi')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={LIST} /></svg>
          Suivi des souscriptions
        </button>
        <button className={`tab-btn ${onglet === 'nouvelle' ? 'active' : ''}`} onClick={() => setOnglet('nouvelle')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={PLUS} /></svg>
          Nouvelle souscription
        </button>
      </div>

      {onglet === 'suivi' ? <SuiviSouscriptionsTab /> : <NouvelleSouscriptionTab />}
    </section>
  );
}
