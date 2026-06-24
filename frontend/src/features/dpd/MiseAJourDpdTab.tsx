import { useState } from 'react';
import { chargerAccord, dateLongue, formaterDa, synchroniserAccord } from './api';
import { AccordProassur, Echeance } from './types';

function recap(echeances: Echeance[]) {
  const total = echeances.reduce((s, e) => s + e.montant, 0);
  const reglees = echeances.filter((e) => e.statutReglement === 'REGLEE');
  const montantRegle = reglees.reduce((s, e) => s + e.montant, 0);
  return {
    total,
    nbReglees: reglees.length,
    montantRegle,
    nbRestantes: echeances.length - reglees.length,
    montantRestant: total - montantRegle,
  };
}

function reglementCell(e: Echeance) {
  if (e.statutReglement === 'REGLEE') {
    return <span className="stat pay-ok">✓ Réglée le {e.dateReglement ? dateLongue(e.dateReglement) : '—'}</span>;
  }
  if (e.statutReglement === 'ECHUE') {
    return <span className="stat pay-echue">● Restante · échue</span>;
  }
  return <span className="stat pay-rest">○ Restante · à échoir</span>;
}

export default function MiseAJourDpdTab() {
  const [code, setCode] = useState('');
  const [accord, setAccord] = useState<AccordProassur | null>(null);
  const [chargement, setChargement] = useState(false);
  const [sync, setSync] = useState(false);
  const [message, setMessage] = useState<{ type: 'ok' | 'ko'; texte: string } | null>(null);

  async function charger() {
    if (!code.trim()) return;
    setChargement(true);
    setMessage(null);
    setAccord(null);
    try {
      setAccord(await chargerAccord(code.trim()));
    } catch (e) {
      setMessage({ type: 'ko', texte: e instanceof Error ? e.message : 'Accord introuvable' });
    } finally {
      setChargement(false);
    }
  }

  async function mettreAJour() {
    setSync(true);
    setMessage(null);
    try {
      const r = await synchroniserAccord(code.trim());
      setMessage({
        type: 'ok',
        texte: `Dossier mis à jour — version ${r.versionCourante} (historisée, ${r.nbVersions} version(s) au total).`,
      });
    } catch (e) {
      setMessage({ type: 'ko', texte: e instanceof Error ? e.message : 'Erreur' });
    } finally {
      setSync(false);
    }
  }

  const r = accord ? recap(accord.echeances) : null;

  return (
    <div className="form-card">
      <div className="sec-head"><h3>Mise à jour d'un dossier de paiement différé</h3></div>

      <div className="loader">
        <div className="field">
          <label>Code de l'accord</label>
          <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="ex. AC-0231" />
        </div>
        <button className="btn-primary" onClick={charger} disabled={chargement || !code.trim()}>
          {chargement ? 'Chargement…' : '↧ Charger depuis PROASSUR'}
        </button>
        <span className="hint-strong">Récupère le dossier et le nouvel échéancier validé.</span>
      </div>

      {message && <p className={`form-msg ${message.type}`}>{message.texte}</p>}

      {accord && r && (
        <>
          <div className="sec-head" style={{ marginTop: 18 }}>
            <h3>Accord chargé</h3>
            <span className="ro-tag">source PROASSUR</span>
          </div>
          <div className="resume-card">
            <div className="kv"><div className="k">N° Accord</div><div className="v">{accord.resume.noAccord}</div></div>
            <div className="kv"><div className="k">Assuré</div><div className="v">{accord.resume.assure}</div></div>
            <div className="kv"><div className="k">Police / Proposition</div><div className="v">{accord.resume.policeOuProposition}</div></div>
            <div className="kv"><div className="k">Montant prime</div><div className="v">{formaterDa(accord.resume.montantPrime)}</div></div>
            <div className="kv"><div className="k">Statut actuel</div><div className="v">{accord.resume.statut}</div></div>
            <div className="kv"><div className="k">Dernière mise à jour</div><div className="v">{dateLongue(accord.resume.dateDerniereMaj)}</div></div>
          </div>

          <div className="sec-head">
            <h3>Nouvel échéancier validé</h3>
            <span className="ro-tag">renseigné dans PROASSUR · lecture seule</span>
          </div>
          <table className="ech-table">
            <thead>
              <tr>
                <th>N°</th>
                <th>Date d'échéance</th>
                <th>Montant (DA)</th>
                <th>Règlement</th>
              </tr>
            </thead>
            <tbody>
              {accord.echeances.map((e) => (
                <tr key={e.numero} className={e.statutReglement === 'REGLEE' ? 'paid' : ''}>
                  <td className="ech-n">{e.numero}</td>
                  <td>{dateLongue(e.datePrevue)}</td>
                  <td>{e.montant.toLocaleString('fr-DZ')}</td>
                  <td>{reglementCell(e)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="ech-total">
            <span className="l">
              Total {formaterDa(r.total)} · {r.nbReglees} réglées ({formaterDa(r.montantRegle)}) ·{' '}
              {r.nbRestantes} restantes ({formaterDa(r.montantRestant)})
            </span>
            <span className="ok">✓ Conforme à la prime</span>
          </div>

          <div className="form-actions">
            <button className="btn-primary" onClick={mettreAJour} disabled={sync}>
              {sync ? 'Mise à jour…' : '↻ Mettre à jour le dossier'}
            </button>
            <button className="btn-ghost" onClick={() => { setAccord(null); setCode(''); setMessage(null); }}>
              Annuler
            </button>
          </div>

          <div className="note">
            <b>Mise à jour d'un DPD.</b> Renseigne le code de l'accord pour récupérer le dernier
            échéancier validé. L'état de règlement (réglée / échue / à échoir) provient du
            rapprochement PROASSUR × Sage — le module le reflète, il ne l'établit pas. Chaque mise à
            jour est historisée en version.
          </div>
        </>
      )}
    </div>
  );
}
