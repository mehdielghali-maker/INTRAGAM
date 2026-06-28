import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { catalogueSouscription, peutEnvoyer, souscription, Souscription } from '@souscription';
import { ApercusControle } from '@sinistre-ui';
import StatutSouscriptionBadge from './StatutSouscriptionBadge';
import '../sinistre/sinistre.css';

/** Détail / contrôle d'une souscription (AGA) : en-tête + complétude + vignettes + actions. */
export default function DetailSouscriptionPage() {
  const { idLocal } = useParams();
  const navigate = useNavigate();
  const [s, setS] = useState<Souscription | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const charger = useCallback(async () => {
    const liste = await souscription.getSouscriptions();
    setS(liste.find((x) => x.idLocal === idLocal) ?? null);
  }, [idLocal]);

  useEffect(() => {
    void charger();
  }, [charger]);

  if (!s) {
    return (
      <section className="sinistre">
        <button className="retour" onClick={() => navigate('/souscription-auto')}>← Retour au suivi</button>
        <p className="vide">Souscription introuvable.</p>
      </section>
    );
  }

  const complet = peutEnvoyer(s.pieces);

  async function enregistrer() {
    if (!s) return;
    const e = await souscription.enregistrerSouscription(s, []);
    setMessage(`Souscription ${e.reference} enregistrée.`);
    await charger();
  }
  async function marquerIncomplete() {
    if (!s) return;
    await souscription.creerSouscription({ ...s, statut: 'INCOMPLETE' });
    setMessage('Souscription marquée incomplète.');
    await charger();
  }

  return (
    <section className="sinistre">
      <button className="retour" onClick={() => navigate('/souscription-auto')}>← Retour au suivi</button>
      {message && <p style={{ color: 'var(--vert)', fontWeight: 600, marginBottom: 12 }}>{message}</p>}

      <div className="detail-head">
        <div><div className="dh-k">Référence</div><div className="dh-v">{s.reference}</div></div>
        <div><div className="dh-k">Police</div><div className="dh-v">{s.numeroPolice}</div></div>
        <div><div className="dh-k">Client</div><div className="dh-v">{s.nomClient}</div></div>
        <div><div className="dh-k">Véhicule</div><div className="dh-v">{s.vehicule.marque ?? '—'} {s.vehicule.immatriculation ?? ''}</div></div>
        <div><div className="dh-k">Produit</div><div className="dh-v">Automobile</div></div>
        <div><div className="dh-k">Statut</div><StatutSouscriptionBadge statut={s.statut} /></div>
      </div>

      <ApercusControle catalogue={catalogueSouscription} contexte={{}} pieces={s.pieces} />

      {s.statut !== 'ENREGISTREE' && (
        <div className="form-actions">
          <button className="btn-primary" disabled={!complet} onClick={enregistrer}>Enregistrer la souscription</button>
          <button className="btn-ghost" onClick={marquerIncomplete}>Marquer incomplète</button>
          {!complet && <span className="hint">Enregistrement impossible : pièces obligatoires manquantes.</span>}
        </div>
      )}
    </section>
  );
}
