import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { brouillonsLocaux, Declaration, rattacherDeclarationsEnAttente } from '@decsin';
import NouvelleDeclarationTab from './NouvelleDeclarationTab';
import SuiviDeclarationsTab from './SuiviDeclarationsTab';
import './sinistre.css';

const PLUS = 'M12 5v14M5 12h14';
const LIST = 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01';

type Onglet = 'suivi' | 'nouvelle';

/**
 * Déclaration de sinistre — face AGA. Modèle PRÉSENTIEL par défaut : l'AGA saisit en agence et peut
 * enregistrer en BROUILLON (reprise possible), puis valide → PROASSUR. L'envoi d'un lien au client
 * est l'exception. Hérite du contexte d'agence (poste).
 */
export default function SinistrePage() {
  const [onglet, setOnglet] = useState<Onglet>('suivi');
  const [brouillonEdit, setBrouillonEdit] = useState<Declaration | null>(null);
  const [params, setParams] = useSearchParams();

  // Rattache les déclarations validées hors-ligne, au chargement et au retour du réseau.
  useEffect(() => {
    void rattacherDeclarationsEnAttente();
    const surReseau = () => void rattacherDeclarationsEnAttente();
    window.addEventListener('online', surReseau);
    return () => window.removeEventListener('online', surReseau);
  }, []);

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
      <div className="page-h">Déclaration de sinistre</div>
      <p className="page-sub">
        Cas normal : le client se présente à l'agence, l'AGA saisit la déclaration (brouillon possible),
        contrôle puis valide → rattachement à PROASSUR (N° de sinistre). Le client peut aussi remplir à
        distance via un lien, en exception.
      </p>

      <div className="flow-strip">
        <div className="flow-step"><span className="s">1 · Brouillon</span><span className="src">saisie AGA en agence (présentiel)</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">2 · À valider</span><span className="src">dossier complet, contrôle AGA</span></div>
        <span className="flow-arrow">→</span>
        <div className="flow-step"><span className="s">3 · Validée</span><span className="src">rattachée à PROASSUR · N° sinistre</span></div>
        <span className="flow-arrow">·</span>
        <div className="flow-step"><span className="s">Lien client</span><span className="src">exception : à distance</span></div>
      </div>

      <div className="tabs">
        <button className={`tab-btn ${onglet === 'suivi' ? 'active' : ''}`} onClick={() => setOnglet('suivi')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={LIST} /></svg>
          Suivi des déclarations
        </button>
        <button
          className={`tab-btn ${onglet === 'nouvelle' ? 'active' : ''}`}
          onClick={() => { setBrouillonEdit(null); setOnglet('nouvelle'); }}
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}><path d={PLUS} /></svg>
          Nouvelle déclaration
        </button>
      </div>

      {onglet === 'suivi' ? (
        <SuiviDeclarationsTab />
      ) : (
        <NouvelleDeclarationTab brouillonInitial={brouillonEdit} onFini={() => setBrouillonEdit(null)} />
      )}
    </section>
  );
}
