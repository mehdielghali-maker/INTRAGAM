import { useEffect, useState } from 'react';
import { rattacherDeclarationsEnAttente } from '@decsin';
import NouvelleDeclarationTab from './NouvelleDeclarationTab';
import SuiviDeclarationsTab from './SuiviDeclarationsTab';
import './sinistre.css';

const PLUS = 'M12 5v14M5 12h14';
const LIST = 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01';

type Onglet = 'suivi' | 'nouvelle';

/**
 * Déclaration de sinistre — face AGA. Frise du flux (Initiée → Remplie → À valider → Validée)
 * + onglets « Suivi » et « Nouvelle déclaration ». Hérite du contexte d'agence (poste).
 */
export default function SinistrePage() {
  const [onglet, setOnglet] = useState<Onglet>('suivi');

  // Rattache les déclarations validées hors-ligne, au chargement et au retour du réseau.
  useEffect(() => {
    void rattacherDeclarationsEnAttente();
    const surReseau = () => void rattacherDeclarationsEnAttente();
    window.addEventListener('online', surReseau);
    return () => window.removeEventListener('online', surReseau);
  }, []);

  return (
    <section className="sinistre">
      <div className="page-h">Déclaration de sinistre</div>
      <p className="page-sub">
        Saisissez une déclaration ou envoyez un lien au client. Contrôlez puis validez pour
        rattacher le dossier à PROASSUR (qui renvoie le N° de sinistre).
      </p>

      <div className="flow-strip">
        <div className="flow-step"><span className="s">1 · Initiée</span><span className="src">AGA ou lien client (code)</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">2 · Remplie</span><span className="src">formulaire + photos</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">3 · À valider</span><span className="src">contrôle par l'AGA</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">4 · Validée</span><span className="src">rattachée à PROASSUR · N° sinistre</span></div>
      </div>

      <div className="tabs">
        <button className={`tab-btn ${onglet === 'suivi' ? 'active' : ''}`} onClick={() => setOnglet('suivi')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={LIST} /></svg>
          Suivi des déclarations
        </button>
        <button className={`tab-btn ${onglet === 'nouvelle' ? 'active' : ''}`} onClick={() => setOnglet('nouvelle')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={PLUS} /></svg>
          Nouvelle déclaration
        </button>
      </div>

      {onglet === 'suivi' ? <SuiviDeclarationsTab /> : <NouvelleDeclarationTab />}
    </section>
  );
}
