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
    return (
      <span className="stat pay-ok">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="M5 12l5 5L20 6" /></svg>
        Réglée le {e.dateReglement ? dateLongue(e.dateReglement) : '—'}
      </span>
    );
  }
  if (e.statutReglement === 'ECHUE') {
    return (
      <span className="stat pay-echue">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="9" /><path d="M12 8v4M12 16h.01" /></svg>
        Restante · échue
      </span>
    );
  }
  return <span className="stat pay-rest">Restante · à échoir</span>;
}

export default function MiseAJourDpdTab() {
  const [code, setCode] = useState('');
  const [accord, setAccord] = useState<AccordProassur | null>(null);
  const [commentaire, setCommentaire] = useState('');
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
      const r = await synchroniserAccord(code.trim(), commentaire.trim() || undefined);
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
      <div className="sso-strip">
        <span className="ms" aria-hidden>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M21 12a9 9 0 1 1-3-6.7M21 4v5h-5" />
          </svg>
        </span>
        <div className="who">
          <b>Mise à jour d'un dossier de paiement différé</b>
          <span>À utiliser lorsqu'un nouvel échéancier a été validé dans PROASSUR</span>
        </div>
      </div>

      <div className="loader">
        <div className="field">
          <label>Code de l'accord <span className="req">*</span></label>
          <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="Ex. ACC-2026-00231" />
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
            <span className="tile" aria-hidden>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <rect x="5" y="3" width="14" height="18" rx="2" />
                <path d="M9 8h6M9 12h6" />
              </svg>
            </span>
            <h3>Accord chargé</h3>
            <span className="ro-tag">source PROASSUR</span>
          </div>
          <div className="grid cols4">
            <Ro label="N° Accord" value={accord.resume.noAccord} />
            <Ro label="Assuré" value={accord.resume.assure} span2 />
            <Ro label="Police / Proposition" value={accord.resume.policeOuProposition} />
            <Ro label="Montant prime" value={formaterDa(accord.resume.montantPrime)} />
            <Ro label="Statut actuel" value={accord.resume.statut} />
            <Ro label="Dernière mise à jour" value={dateLongue(accord.resume.dateDerniereMaj)} span2 />
          </div>

          <div className="sec-head">
            <span className="tile" aria-hidden>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <rect x="4" y="5" width="16" height="16" rx="2" />
                <path d="M4 9h16M8 3v4M16 3v4" />
              </svg>
            </span>
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

          <div className="grid" style={{ marginTop: 18 }}>
            <div className="field full">
              <label>Commentaire</label>
              <textarea
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
                placeholder="Motif de la mise à jour de l'échéancier…"
              />
            </div>
          </div>

          <div className="form-actions">
            <button className="btn-primary" onClick={mettreAJour} disabled={sync}>
              {sync ? 'Mise à jour…' : '↻ Mettre à jour le dossier'}
            </button>
            <button
              className="btn-ghost"
              onClick={() => { setAccord(null); setCode(''); setCommentaire(''); setMessage(null); }}
            >
              Annuler
            </button>
          </div>

          <div className="note">
            <b>Mise à jour d'un DPD.</b> Quand un nouvel échéancier est validé dans PROASSUR,
            saisissez le <b>code de l'accord</b> : le module récupère le dossier et le nouvel
            échéancier (lecture seule, source PROASSUR) et met à jour le suivi. L'échéancier reste
            porté par PROASSUR — le module ne fait que le refléter ; chaque mise à jour est
            historisée en version.
          </div>
        </>
      )}
    </div>
  );
}

function Ro({ label, value, span2 }: { label: string; value?: string | null; span2?: boolean }) {
  return (
    <div className={`field ${span2 ? 'span2' : ''}`}>
      <label>{label}</label>
      <input className="ro" readOnly value={value ?? ''} placeholder="—" />
    </div>
  );
}
