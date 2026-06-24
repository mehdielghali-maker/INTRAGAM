import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FILTRES_DPD, dateCourte, formaterDa, listerDpd } from './api';
import { DemandeDpd, StatutDpd } from './types';
import StatutDpdBadge from './StatutDpdBadge';

function action(d: DemandeDpd) {
  switch (d.statut) {
    case 'ENVOYEE':
      return <span className="act-txt">En attente de prise en charge</span>;
    case 'EN_VALIDATION':
      return <span className="act-txt">{d.validateurNom ?? 'Non affecté'} — en cours</span>;
    case 'ACCORDEE':
      return (
        <Link className="act-link" to="/echeanciers">
          ↗ Voir dans Suivi des échéanciers
        </Link>
      );
    case 'REFUSEE':
      return <span className="act-txt">Motif : {d.motifRefus}</span>;
    default:
      return <span className="act-txt">Brouillon</span>;
  }
}

function ligneObjet(d: DemandeDpd) {
  const ref = d.souscription.noPolice
    ? `Police ${d.souscription.noPolice} · Avenant`
    : `Prop. ${d.souscription.noProposition} · ${d.souscription.branche}`;
  return ref;
}

export default function SuiviDpdTab({ onNouvelle }: { onNouvelle: () => void }) {
  const [demandes, setDemandes] = useState<DemandeDpd[]>([]);
  const [filtre, setFiltre] = useState<StatutDpd | ''>('');
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(async () => {
    setErreur(null);
    try {
      setDemandes(await listerDpd(filtre));
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Erreur inconnue');
    }
  }, [filtre]);

  useEffect(() => {
    charger();
  }, [charger]);

  return (
    <section>
      <div className="toolbar">
        <div className="filters">
          {FILTRES_DPD.map((f) => (
            <button
              key={f.cle || 'toutes'}
              className={`fchip ${filtre === f.cle ? 'active' : ''}`}
              onClick={() => setFiltre(f.cle)}
            >
              {f.libelle}
            </button>
          ))}
        </div>
        <button className="btn-primary" onClick={onNouvelle}>
          + Nouvelle demande
        </button>
      </div>

      {erreur && <p className="form-msg ko">{erreur}</p>}

      {/* Desktop : tableau */}
      <table className="dpd-table">
        <thead>
          <tr>
            <th>N° Demande</th>
            <th>Assuré / Proposition</th>
            <th>Montant prime</th>
            <th>Durée</th>
            <th>Date</th>
            <th>Statut</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {demandes.map((d) => (
            <tr key={d.id}>
              <td className="ref">{d.reference ?? '—'}</td>
              <td className="objet">
                <div className="t">{d.infoClient.nomAssure}</div>
                <div className="s">{ligneObjet(d)}</div>
              </td>
              <td className="amt">{formaterDa(d.souscription.montantPrime)}</td>
              <td>{d.souscription.dureeContratMois} mois</td>
              <td>{dateCourte(d.dateMaj)}</td>
              <td>
                <StatutDpdBadge statut={d.statut} libelle={d.statutLibelle} />
              </td>
              <td>{action(d)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Mobile : cartes */}
      <div className="dpd-cards">
        {demandes.map((d) => (
          <div className="dpd-card" key={d.id}>
            <div className="l1">
              <span className="ref">{d.reference ?? '—'}</span>
              <StatutDpdBadge statut={d.statut} libelle={d.statutLibelle} />
            </div>
            <div className="who">{d.infoClient.nomAssure}</div>
            <div className="meta">
              {ligneObjet(d)} · {formaterDa(d.souscription.montantPrime)} · {d.souscription.dureeContratMois} mois ·{' '}
              {dateCourte(d.dateMaj)}
            </div>
            <div className="l-act">{action(d)}</div>
          </div>
        ))}
      </div>

      {demandes.length === 0 && <p className="act-txt" style={{ marginTop: 16 }}>Aucune demande.</p>}

      <div className="note">
        <b>Articulation des deux fonctions.</b> Ce module gère la <b>demande d'accord</b> ; une fois
        <b> Accordée</b>, l'exécution de l'échéancier (honorée / en retard / rompue) est suivie dans
        <b> « Suivi des échéanciers »</b>.
      </div>
    </section>
  );
}
