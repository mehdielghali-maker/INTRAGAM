import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  DossierCheque,
  LIBELLE_STATUT,
  StatutCheque,
  listerCheques,
} from '../../api/cheques';
import StatutBadge from './StatutBadge';

const STATUTS: StatutCheque[] = [
  'EMIS',
  'IMPRIME',
  'REMIS_AGENCE',
  'REMIS_BENEFICIAIRE',
  'ENCAISSE',
  'RETOURNE',
];

export default function ChequeListPage() {
  const [dossiers, setDossiers] = useState<DossierCheque[]>([]);
  const [statut, setStatut] = useState<StatutCheque | ''>('');
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);

  const charger = useCallback(async () => {
    setChargement(true);
    setErreur(null);
    try {
      setDossiers(await listerCheques({ statut }));
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Erreur inconnue');
    } finally {
      setChargement(false);
    }
  }, [statut]);

  useEffect(() => {
    charger();
  }, [charger]);

  return (
    <section>
      <h1>Dossiers de chèques</h1>

      <div className="filtres">
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
        <button onClick={charger} disabled={chargement}>
          {chargement ? 'Chargement…' : 'Rafraîchir'}
        </button>
      </div>

      {erreur && <p className="erreur">{erreur}</p>}

      {!erreur && dossiers.length === 0 && !chargement && (
        <p className="vide">
          Aucun dossier. Simulez une émission PROASSUR via
          <code> POST /api/mock/proassur/cheques</code> (voir README).
        </p>
      )}

      {dossiers.length > 0 && (
        <table className="table-cheques">
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
                <td className="montant">
                  {d.montant.toLocaleString('fr-DZ')} {d.devise}
                </td>
                <td>{d.beneficiaire}</td>
                <td>{d.agence}</td>
                <td>{d.dateEmission}</td>
                <td>
                  <StatutBadge statut={d.statut} />
                </td>
                <td>
                  <Link to={`/cheques/${d.id}`}>Détail →</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
