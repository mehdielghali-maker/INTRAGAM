import { useCallback, useEffect, useState } from 'react';
import { FILTRES_STATUT, listerDemandes, marquerSansSuite, simulerQuittance } from './api';
import { DemandeCotation, StatutCotation } from './types';
import StatutCotationBadge from './StatutCotationBadge';
import SouscripteurCell from './SouscripteurCell';

function dateCourte(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export default function SuiviDemandesTab({ onNouvelle }: { onNouvelle: () => void }) {
  const [demandes, setDemandes] = useState<DemandeCotation[]>([]);
  const [filtre, setFiltre] = useState<StatutCotation | ''>('');
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);

  const charger = useCallback(async () => {
    setChargement(true);
    setErreur(null);
    try {
      setDemandes(await listerDemandes(filtre));
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Erreur inconnue');
    } finally {
      setChargement(false);
    }
  }, [filtre]);

  useEffect(() => {
    charger();
  }, [charger]);

  async function quittance(reference: string) {
    await simulerQuittance(reference);
    setTimeout(charger, 600); // laisser le retour PROASSUR se propager via le bus
  }

  async function sansSuite(id: string) {
    await marquerSansSuite(id, 'Devis non retenu par le client');
    charger();
  }

  function actionCell(d: DemandeCotation) {
    switch (d.statut) {
      case 'ENVOYEE':
        return <span className="act-txt">En attente de prise en charge</span>;
      case 'EN_COURS':
        return <span className="act-txt">Prise en charge — devis en préparation</span>;
      case 'A_FINALISER':
        return (
          <div>
            <a
              className="act-link"
              href="#"
              title={`Devis ${d.referenceDevis ?? ''} — à consulter dans PROASSUR`}
              onClick={(e) => e.preventDefault()}
            >
              ↗ Retrouver sur PROASSUR
            </a>
            <span className="act-prop">N° prop. {d.numeroProposition}</span>
            {d.reference && (
              <span className="act-prop">
                <a href="#" onClick={(e) => { e.preventDefault(); quittance(d.reference!); }}>
                  · simuler quittance (mock)
                </a>
                {' · '}
                <a href="#" onClick={(e) => { e.preventDefault(); sansSuite(d.id); }}>
                  sans suite
                </a>
              </span>
            )}
          </div>
        );
      case 'AFFAIRE_GAGNEE':
        return (
          <span className="act-win">
            Police {d.numeroPolice}
            <span className="sub">
              Quittance imprimée{d.dateQuittance ? ` · ${dateCourte(d.dateQuittance)}` : ''}
            </span>
          </span>
        );
      case 'SANS_SUITE':
        return <span className="act-txt">{d.motifSansSuite}</span>;
      default:
        return <span className="act-txt">Brouillon</span>;
    }
  }

  return (
    <section>
      <div className="toolbar">
        <div className="filters">
          {FILTRES_STATUT.map((f) => (
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

      <table className="cot-table">
        <thead>
          <tr>
            <th>N° Demande</th>
            <th>Objet</th>
            <th>Souscripteur</th>
            <th>Date</th>
            <th>Statut</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {demandes.map((d) => (
            <tr key={d.id}>
              <td className="cot-ref">{d.reference ?? '—'}</td>
              <td className="objet">
                <div className="t">{d.objet}</div>
                <div className="s">
                  {d.renouvellement ? `Renouvellement${d.numeroPolice ? ` · ${d.numeroPolice}` : ''}` : 'Affaire nouvelle'}
                </div>
              </td>
              <td>
                <SouscripteurCell nom={d.souscripteurNom} initiales={d.souscripteurInitiales} />
              </td>
              <td>{dateCourte(d.dateMaj)}</td>
              <td>
                <StatutCotationBadge statut={d.statut} libelle={d.statutLibelle} />
              </td>
              <td>{actionCell(d)}</td>
            </tr>
          ))}
          {demandes.length === 0 && !chargement && (
            <tr>
              <td colSpan={6} className="act-txt" style={{ textAlign: 'center', padding: 24 }}>
                Aucune demande. Créez-en une via « Nouvelle demande ».
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <div className="note">
        <b>Lecture des statuts.</b> « En cours » et le nom du <b>souscripteur</b> sont remontés
        via l'<b>API BPM (OneBase)</b>. « À finaliser » signale que le devis est prêt : il se
        consulte <b>dans PROASSUR</b>. « Affaire gagnée » provient de PROASSUR à l'impression de
        la quittance. Le poste n'affiche que statuts et références, jamais le contenu du devis.
      </div>
    </section>
  );
}
