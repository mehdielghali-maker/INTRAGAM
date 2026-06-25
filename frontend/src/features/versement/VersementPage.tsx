import { useCallback, useEffect, useState } from 'react';
import { useAgence } from '../../app/AgencyContext';
import { getOptions, getSituation, listerVersements } from './api';
import { SituationMois, VersementOptions, VersementResponse } from './types';
import VersementForm from './VersementForm';
import SuiviVersementsTable from './SuiviVersementsTable';
import './versement.css';

export default function VersementPage() {
  const { contexte } = useAgence();
  const codeAgence = contexte?.agenceActive.code;

  const [options, setOptions] = useState<VersementOptions | null>(null);
  const [mois, setMois] = useState<string>('');
  const [situation, setSituation] = useState<SituationMois | null>(null);
  const [situationErreur, setSituationErreur] = useState<string | null>(null);
  const [versements, setVersements] = useState<VersementResponse[]>([]);
  const [listeErreur, setListeErreur] = useState<string | null>(null);

  // Options (banques + mois) une fois ; le 1er mois devient le mois sélectionné.
  useEffect(() => {
    getOptions()
      .then((o) => {
        setOptions(o);
        if (o.mois.length > 0) setMois((m) => m || o.mois[0].valeur);
      })
      .catch(() => undefined);
  }, []);

  // Situation rechargée au changement de mois ET au changement d'agence active.
  const chargerSituation = useCallback(async () => {
    if (!mois) return;
    setSituationErreur(null);
    try {
      setSituation(await getSituation(mois));
    } catch (e) {
      setSituation(null);
      setSituationErreur(e instanceof Error ? e.message : 'Situation indisponible');
    }
  }, [mois, codeAgence]);

  useEffect(() => {
    chargerSituation();
  }, [chargerSituation]);

  // Liste des versements déposés (dépend de l'agence active).
  const chargerListe = useCallback(async () => {
    setListeErreur(null);
    try {
      setVersements(await listerVersements());
    } catch (e) {
      setListeErreur(e instanceof Error ? e.message : 'Liste indisponible');
    }
  }, [codeAgence]);

  useEffect(() => {
    chargerListe();
  }, [chargerListe]);

  // Après soumission : on rafraîchit la situation et la liste.
  function onSuccess() {
    chargerSituation();
    chargerListe();
  }

  return (
    <section>
      <h1 className="page-h">Versement bancaire — preuve de paiement</h1>
      <p className="page-sub">
        Déposez le reçu de versement justifiant l'encaissement des primes émises sur un mois donné.
        La pièce est transmise au BPM et rattachée à la situation financière de l'agence et du mois
        choisis.
      </p>

      <div className="form-card" style={{ marginBottom: 18 }}>
        <div className="sso-strip">
          <span className="ms" aria-hidden>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
              <path d="M3 21h18M5 21V7l7-4 7 4v14" />
            </svg>
          </span>
          <div className="who">
            <b>
              Agence active : {contexte?.agenceActive.nom ?? '…'} — {codeAgence ?? '…'}
            </b>
            <span>
              Héritée du contexte de session. Pour changer d'agence, utilisez le commutateur en haut
              de l'écran.
            </span>
          </div>
        </div>
      </div>

      <VersementForm
        options={options}
        situation={situation}
        situationErreur={situationErreur}
        mois={mois}
        onMoisChange={setMois}
        onSuccess={onSuccess}
      />

      <div className="sec-title2">Versements déposés</div>
      {listeErreur && <p className="form-msg ko">{listeErreur}</p>}
      <SuiviVersementsTable versements={versements} />

      <div className="note">
        <b>Cohérence avec la situation financière.</b> Le montant versé vient en déduction du{' '}
        <b>« reste à régulariser »</b> du mois (encaissé − versé). Les chiffres émis/encaissés sont
        lus dans PROASSUR, les versements dans Sage : le module rattache la preuve au mois et à
        l'agence, il ne recalcule pas la comptabilité. Le statut (Déposé → En contrôle → Validé /
        Rejeté) est porté par le BPM.
      </div>
    </section>
  );
}
