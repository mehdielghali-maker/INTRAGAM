import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAgence } from '../../app/AgencyContext';
import { DossierCheque, LIBELLE_STATUT, StatutCheque, listerCheques } from '../../api/cheques';
import StatutBadge from './StatutBadge';
import './cheques.css';

const STATUTS: StatutCheque[] = [
  'EMIS',
  'IMPRIME',
  'REMIS_AGENCE',
  'REMIS_BENEFICIAIRE',
  'ENCAISSE',
  'RETOURNE',
];

export default function ChequeListPage() {
  const { contexte } = useAgence();
  // Clé de sélection : l'agence active, ou « CONSOLIDE » en vue d'ensemble.
  const selection = contexte
    ? contexte.consolideActif
      ? 'CONSOLIDE'
      : contexte.agenceActive?.code ?? null
    : null;

  const [dossiers, setDossiers] = useState<DossierCheque[]>([]);
  const [statut, setStatut] = useState<StatutCheque | ''>('');
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);

  // Rafraîchissement AUTOMATIQUE : au changement d'agence active et de filtre statut.
  useEffect(() => {
    if (!selection) return;
    let annule = false;
    setChargement(true);
    setErreur(null);
    listerCheques({ statut: statut || undefined })
      .then((d) => {
        if (!annule) setDossiers(d);
      })
      .catch((e) => {
        if (!annule) setErreur(e instanceof Error ? e.message : 'Erreur de chargement');
      })
      .finally(() => {
        if (!annule) setChargement(false);
      });
    return () => {
      annule = true;
    };
  }, [statut, selection]);

  const consolide = contexte?.consolideActif ?? false;
  const agenceNom = contexte?.agenceActive?.nom;

  return (
    <section>
      <h1 className="page-h">Suivi des chèques</h1>
      <p className="page-sub">
        {consolide
          ? 'Vue consolidée — chèques de toutes vos agences.'
          : `Chèques de l'agence active${agenceNom ? ` : ${agenceNom}` : ''}. Le changement d'agence (commutateur en haut) met la liste à jour automatiquement.`}
      </p>

      <div className="ch-filtres">
        <label>
          Statut
          <select value={statut} onChange={(e) => setStatut(e.target.value as StatutCheque | '')}>
            <option value="">Tous</option>
            {STATUTS.map((s) => (
              <option key={s} value={s}>
                {LIBELLE_STATUT[s]}
              </option>
            ))}
          </select>
        </label>
        {chargement && <span className="ch-chargement">Chargement…</span>}
      </div>

      {erreur && <p className="ch-erreur">{erreur}</p>}

      {!erreur && dossiers.length === 0 && !chargement && (
        <p className="ch-vide">
          Aucun chèque pour cette sélection. Simulez une émission PROASSUR via{' '}
          <code>POST /api/mock/proassur/cheques</code>.
        </p>
      )}

      {dossiers.length > 0 && (
        <>
          {/* Desktop : tableau */}
          <table className="list">
            <thead>
              <tr>
                <th>Référence</th>
                <th>Montant</th>
                <th>Bénéficiaire</th>
                <th>Agence</th>
                <th>Émis le</th>
                <th>Statut</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {dossiers.map((d) => (
                <tr key={d.id}>
                  <td>{d.reference}</td>
                  <td className="amt">
                    {d.montant.toLocaleString('fr-DZ')} {d.devise}
                  </td>
                  <td>{d.beneficiaire}</td>
                  <td>{d.agence}</td>
                  <td>{d.dateEmission}</td>
                  <td>
                    <StatutBadge statut={d.statut} />
                  </td>
                  <td>
                    <Link className="lien-detail" to={`/cheques/${d.id}`}>
                      Détail →
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Mobile : cartes */}
          <div className="ch-cards">
            {dossiers.map((d) => (
              <div className="ch-card" key={d.id}>
                <div className="l1">
                  <span className="ref">{d.reference}</span>
                  <StatutBadge statut={d.statut} />
                </div>
                <div className="who">
                  {d.montant.toLocaleString('fr-DZ')} {d.devise}
                </div>
                <div className="meta">
                  {d.beneficiaire} · {d.agence} · émis le {d.dateEmission}
                </div>
                <Link className="lien-detail" to={`/cheques/${d.id}`}>
                  Détail →
                </Link>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}
