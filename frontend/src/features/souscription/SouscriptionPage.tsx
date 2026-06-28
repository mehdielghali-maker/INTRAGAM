import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { brouillonsLocaux, Souscription } from '@souscription';
import NouvelleSouscriptionTab from './NouvelleSouscriptionTab';
import SuiviSouscriptionsTab from './SuiviSouscriptionsTab';
import '../sinistre/sinistre.css';

const PLUS = 'M12 5v14M5 12h14';
const LIST = 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01';

type Onglet = 'suivi' | 'nouvelle';

/**
 * Souscription auto — face AGA. Modèle PRÉSENTIEL par défaut : l'AGA saisit en agence et peut
 * enregistrer en BROUILLON (reprise possible), puis valide → enregistrement GAM. Réutilise le
 * design de la feature sinistre.
 */
export default function SouscriptionPage() {
  const [onglet, setOnglet] = useState<Onglet>('suivi');
  const [brouillonEdit, setBrouillonEdit] = useState<Souscription | null>(null);
  const [params, setParams] = useSearchParams();

  // Reprise d'un brouillon via ?reprendre=<idLocal> (depuis le suivi ou le détail).
  useEffect(() => {
    const id = params.get('reprendre');
    if (!id) return;
    void brouillonsLocaux.obtenir(id).then((b) => {
      if (b) {
        setBrouillonEdit(b);
        setOnglet('nouvelle');
      }
      params.delete('reprendre');
      setParams(params, { replace: true });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params]);

  return (
    <section className="sinistre">
      <div className="page-h">Souscription auto</div>
      <p className="page-sub">
        Cas normal : le client se présente à l'agence, l'AGA recherche la police et saisit la
        souscription (brouillon possible), contrôle puis valide → enregistrement GAM.
      </p>

      <div className="flow-strip">
        <div className="flow-step"><span className="s">1 · Brouillon</span><span className="src">saisie AGA en agence (présentiel)</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">2 · À valider</span><span className="src">dossier complet, contrôle AGA</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">3 · Enregistrée</span><span className="src">validée · transmise à GAM</span></div>
      </div>

      <div className="tabs">
        <button className={`tab-btn ${onglet === 'suivi' ? 'active' : ''}`} onClick={() => setOnglet('suivi')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={LIST} /></svg>
          Suivi des souscriptions
        </button>
        <button
          className={`tab-btn ${onglet === 'nouvelle' ? 'active' : ''}`}
          onClick={() => { setBrouillonEdit(null); setOnglet('nouvelle'); }}
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={PLUS} /></svg>
          Nouvelle souscription
        </button>
      </div>

      {onglet === 'suivi' ? (
        <SuiviSouscriptionsTab />
      ) : (
        <NouvelleSouscriptionTab brouillonInitial={brouillonEdit} onFini={() => setBrouillonEdit(null)} />
      )}
    </section>
  );
}
