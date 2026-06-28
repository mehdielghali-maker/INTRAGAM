import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { brouillonsLocaux, catalogueSouscription, peutEnvoyer, souscription, Souscription } from '@souscription';
import { ApercusControle, VerificationPlaque } from '@sinistre-ui';
import StatutSouscriptionBadge from './StatutSouscriptionBadge';
import '../sinistre/sinistre.css';

/** Détail / contrôle d'une souscription (AGA) : en-tête + complétude + vignettes + actions. */
export default function DetailSouscriptionPage() {
  const { idLocal } = useParams();
  const navigate = useNavigate();
  const [s, setS] = useState<Souscription | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const charger = useCallback(async () => {
    // Un brouillon présentiel vit en local ; les autres dans GAM.
    const [brouillons, envoyees] = await Promise.all([brouillonsLocaux.lister(), souscription.getSouscriptions()]);
    setS([...brouillons, ...envoyees].find((x) => x.idLocal === idLocal) ?? null);
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
    const e = await souscription.enregistrerSouscription({ ...s, statut: 'A_VALIDER' }, []);
    setMessage(`Souscription ${e.reference} validée et enregistrée.`);
    await charger();
  }
  async function renvoyer() {
    if (!s) return;
    await souscription.creerSouscription({ ...s, statut: 'RELANCE' });
    setMessage('Souscription renvoyée au client pour correction.');
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

      <VerificationPlaque pieces={s.pieces} immatriculation={s.vehicule.immatriculation} />

      {s.statut === 'BROUILLON' && (
        <div className="form-actions">
          <button className="btn-primary" onClick={() => navigate(`/souscription-auto?reprendre=${encodeURIComponent(s.idLocal)}`)}>
            Reprendre la saisie
          </button>
          <span className="hint">Brouillon présentiel — non transmis à GAM tant qu'il n'est pas validé.</span>
        </div>
      )}

      {s.statut === 'A_VALIDER' && (
        <div className="form-actions">
          <button className="btn-primary" disabled={!complet} onClick={enregistrer}>Valider la souscription</button>
          <button className="btn-ghost" onClick={renvoyer}>Renvoyer au client</button>
          {!complet && <span className="hint">Validation impossible : pièces obligatoires manquantes.</span>}
        </div>
      )}
    </section>
  );
}
