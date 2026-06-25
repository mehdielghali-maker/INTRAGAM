import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAgence } from '../../app/AgencyContext';
import {
  DossierCheque,
  LIBELLE_STATUT,
  StatutCheque,
  faireAvancerStatut,
  obtenirCheque,
} from '../../api/cheques';
import StatutBadge from './StatutBadge';

export default function ChequeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { contexte } = useAgence();
  const consolide = contexte?.consolideActif ?? false;
  const [dossier, setDossier] = useState<DossierCheque | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);

  const charger = useCallback(async () => {
    if (!id) return;
    setErreur(null);
    try {
      setDossier(await obtenirCheque(id));
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Erreur inconnue');
    }
  }, [id]);

  useEffect(() => {
    charger();
  }, [charger]);

  async function avancer(statutCible: StatutCheque) {
    if (!id) return;
    setEnCours(true);
    setErreur(null);
    try {
      setDossier(await faireAvancerStatut(id, statutCible));
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Erreur inconnue');
    } finally {
      setEnCours(false);
    }
  }

  if (erreur && !dossier) {
    return (
      <section>
        <Link to="/">← Retour</Link>
        <p className="erreur">{erreur}</p>
      </section>
    );
  }

  if (!dossier) {
    return <p>Chargement…</p>;
  }

  const terminal = dossier.prochainsStatuts.length === 0;

  return (
    <section className="detail">
      <Link to="/">← Retour à la liste</Link>
      <h1>Chèque {dossier.reference}</h1>

      <dl className="fiche">
        <dt>Statut</dt>
        <dd>
          <StatutBadge statut={dossier.statut} />
        </dd>
        <dt>Montant</dt>
        <dd>
          {dossier.montant.toLocaleString('fr-DZ')} {dossier.devise}
        </dd>
        <dt>Bénéficiaire</dt>
        <dd>{dossier.beneficiaire}</dd>
        <dt>Agence</dt>
        <dd>{dossier.agence}</dd>
        <dt>Date d'émission</dt>
        <dd>{dossier.dateEmission}</dd>
        <dt>Dernière mise à jour</dt>
        <dd>{new Date(dossier.dateDerniereMaj).toLocaleString('fr-DZ')}</dd>
      </dl>

      {erreur && <p className="erreur">{erreur}</p>}

      {terminal ? (
        <p className="info-terminal">
          Statut terminal atteint. Le poste a publié <code>ChequeStatutFinalise</code> :
          le règlement est réécrit dans PROASSUR (boucle fermée).
        </p>
      ) : (
        <div className="actions">
          <span>Faire avancer&nbsp;:</span>
          {dossier.prochainsStatuts.map((s) => (
            <button key={s} onClick={() => avancer(s)} disabled={enCours || consolide}>
              {LIBELLE_STATUT[s]}
            </button>
          ))}
          {consolide && (
            <span className="conso-note">
              Action indisponible en vue consolidée — choisissez une agence.
            </span>
          )}
        </div>
      )}
    </section>
  );
}
