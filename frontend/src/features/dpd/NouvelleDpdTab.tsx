import { useEffect, useState } from 'react';
import { brouillonDpd, contexteDpd, dateLongue, envoyerDpd, formaterDa, prefillDpd } from './api';
import { ContexteDpd, CreerDpdPayload, Souscription, TypePersonne } from './types';
import { FichierLocal } from './imageCompression';
import PieceCapture from './PieceCapture';

export default function NouvelleDpdTab({ onEnvoye }: { onEnvoye: () => void }) {
  const [ctx, setCtx] = useState<ContexteDpd | null>(null);
  const [noProposition, setNoProposition] = useState('');
  const [souscription, setSouscription] = useState<Souscription | null>(null);
  const [avenant, setAvenant] = useState(false);

  const [nomAssure, setNomAssure] = useState('');
  const [nomSouscripteur, setNomSouscripteur] = useState('');
  const [telephone, setTelephone] = useState('');
  const [cnrc, setCnrc] = useState('');
  const [typePersonne, setTypePersonne] = useState<TypePersonne>('MORALE');
  const [institutionPublique, setInstitutionPublique] = useState(false);
  const [adresse, setAdresse] = useState('');
  const [commentaire, setCommentaire] = useState('');

  const [rc, setRc] = useState<FichierLocal[]>([]);
  const [autres, setAutres] = useState<FichierLocal[]>([]);

  const [chargement, setChargement] = useState(false);
  const [envoi, setEnvoi] = useState(false);
  const [message, setMessage] = useState<{ type: 'ok' | 'ko'; texte: string } | null>(null);

  useEffect(() => {
    contexteDpd().then(setCtx).catch(() => undefined);
  }, []);

  async function charger() {
    if (!noProposition.trim()) return;
    setChargement(true);
    setMessage(null);
    try {
      const p = await prefillDpd(noProposition.trim());
      setSouscription(p.souscription);
      setNomAssure(p.client.nomAssure);
      setNomSouscripteur(p.client.nomSouscripteur);
      setTelephone(p.client.telephone ?? '');
      setCnrc(p.client.cnrc ?? '');
      setTypePersonne(p.client.typePersonne);
      setInstitutionPublique(p.client.institutionPublique);
      setAdresse(p.client.adresse ?? '');
    } catch (e) {
      setMessage({ type: 'ko', texte: e instanceof Error ? e.message : 'Proposition introuvable' });
      setSouscription(null);
    } finally {
      setChargement(false);
    }
  }

  function payload(): CreerDpdPayload {
    return {
      noProposition: noProposition.trim(),
      avenant,
      commentaire: commentaire.trim() || undefined,
      nomAssure: nomAssure.trim(),
      nomSouscripteur: nomSouscripteur.trim(),
      telephone: telephone.trim() || undefined,
      cnrc: cnrc.trim() || undefined,
      typePersonne,
      institutionPublique,
      adresse: adresse.trim() || undefined,
      fichiersRc: rc.map((f) => f.name),
      fichiersAutres: autres.map((f) => f.name),
    };
  }

  const peutEnvoyer = !!souscription && !!nomAssure.trim() && !!nomSouscripteur.trim() && rc.length > 0;

  async function soumettre(action: 'envoyer' | 'brouillon') {
    setEnvoi(true);
    setMessage(null);
    try {
      const fn = action === 'envoyer' ? envoyerDpd : brouillonDpd;
      const d = await fn(payload());
      if (action === 'envoyer') {
        setMessage({ type: 'ok', texte: `Demande ${d.reference} envoyée pour validation.` });
        setTimeout(onEnvoye, 900);
      } else {
        setMessage({ type: 'ok', texte: 'Brouillon enregistré.' });
      }
    } catch (e) {
      setMessage({ type: 'ko', texte: e instanceof Error ? e.message : 'Erreur' });
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <div className="form-card">
      <div className="sso-strip">
        <span className="ms" aria-hidden>
          <svg viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" /><rect x="13" y="3" width="8" height="8" /><rect x="3" y="13" width="8" height="8" /><rect x="13" y="13" width="8" height="8" /></svg>
        </span>
        <div className="who">
          <b>Demande créée par {ctx?.identite.utilisateur ?? '…'}</b>
          <span>{ctx?.identite.nomAgence} ({ctx?.identite.codeAgence}) · identité Microsoft</span>
        </div>
      </div>

      {/* En-tête */}
      <div className="grid cols4">
        <div className="field">
          <label>N° Demande</label>
          <input className="ro" readOnly value="Auto-généré à l'envoi" />
        </div>
        <div className="field">
          <label>Date Demande</label>
          <input className="ro" readOnly value={dateLongue(new Date().toISOString())} />
        </div>
        <div className="field">
          <label>N° Police</label>
          <input className="ro" readOnly value={souscription?.noPolice ?? '—'} />
        </div>
        <div className="field">
          <label>État</label>
          <input className="ro" readOnly value="Brouillon" />
        </div>
      </div>

      {/* Chargement proposition */}
      <div className="loader" style={{ marginTop: 16 }}>
        <div className="field">
          <label>N° Proposition <span className="req">*</span></label>
          <input
            value={noProposition}
            onChange={(e) => setNoProposition(e.target.value)}
            placeholder="ex. PR-88231"
          />
        </div>
        <button className="btn-primary" onClick={charger} disabled={chargement || !noProposition.trim()}>
          {chargement ? 'Chargement…' : '↧ Charger depuis PROASSUR'}
        </button>
        <label className="checkline">
          <input type="checkbox" checked={avenant} onChange={(e) => setAvenant(e.target.checked)} /> Avenant
        </label>
      </div>

      {/* Souscription (RO) */}
      <div className="sec-head">
        <h3>Souscription</h3>
        <span className="ro-tag">pré-rempli depuis PROASSUR</span>
      </div>
      <div className="grid cols4">
        <Ro label="N° Proposition" value={souscription?.noProposition} />
        <Ro label="Date Proposition" value={souscription ? dateLongue(souscription.dateProposition) : ''} />
        <Ro label="Devis créé par" value={souscription?.devisCreePar} />
        <Ro label="Montant de la prime" value={souscription ? formaterDa(souscription.montantPrime) : ''} />
        <Ro label="Branche" value={souscription?.branche} />
        <Ro label="Date Effet" value={souscription ? dateLongue(souscription.dateEffet) : ''} />
        <Ro label="Date Échéance" value={souscription ? dateLongue(souscription.dateEcheance) : ''} />
        <Ro label="Durée Contrat" value={souscription ? `${souscription.dureeContratMois} mois` : ''} />
      </div>

      {/* Agence & DR (RO) */}
      <div className="sec-head">
        <h3>Agence &amp; Direction Régionale</h3>
        <span className="ro-tag">pré-rempli depuis votre session</span>
      </div>
      <div className="grid cols4">
        <Ro label="Code Agence" value={ctx?.identite.codeAgence} />
        <Ro label="Adresse Mail Agence" value={ctx?.identite.mailAgence} />
        <Ro label="Direction Régionale" value={ctx?.identite.directionRegionale} />
        <Ro label="Mail Direction Régionale" value={ctx?.identite.mailDirectionRegionale} />
      </div>

      {/* Information client (éditable) */}
      <div className="sec-head">
        <h3>Information client</h3>
        <span className="ro-tag">pré-rempli, modifiable si besoin</span>
      </div>
      <div className="grid cols3">
        <div className="field">
          <label>Nom Assuré <span className="req">*</span></label>
          <input value={nomAssure} onChange={(e) => setNomAssure(e.target.value)} />
        </div>
        <div className="field">
          <label>Nom Souscripteur <span className="req">*</span></label>
          <input value={nomSouscripteur} onChange={(e) => setNomSouscripteur(e.target.value)} />
        </div>
        <div className="field">
          <label>N° Tél</label>
          <input value={telephone} onChange={(e) => setTelephone(e.target.value)} />
        </div>
        <div className="field">
          <label>N° CNRC</label>
          <input value={cnrc} onChange={(e) => setCnrc(e.target.value)} />
        </div>
        <div className="field">
          <label>Type Personne</label>
          <div className="radioline">
            <label><input type="radio" name="tp" checked={typePersonne === 'MORALE'} onChange={() => setTypePersonne('MORALE')} /> Personne morale</label>
            <label><input type="radio" name="tp" checked={typePersonne === 'PHYSIQUE'} onChange={() => setTypePersonne('PHYSIQUE')} /> Personne physique</label>
          </div>
        </div>
        <div className="field">
          <label>Institution publique <span className="req">*</span></label>
          <div className="radioline">
            <label><input type="radio" name="ip" checked={institutionPublique} onChange={() => setInstitutionPublique(true)} /> Oui</label>
            <label><input type="radio" name="ip" checked={!institutionPublique} onChange={() => setInstitutionPublique(false)} /> Non</label>
          </div>
        </div>
        <div className="field full">
          <label>Adresse</label>
          <input value={adresse} onChange={(e) => setAdresse(e.target.value)} />
        </div>
      </div>

      {/* Échéancier (RO informatif) */}
      <div className="sec-head">
        <h3>Échéancier</h3>
        <span className="ro-tag">renseigné dans PROASSUR</span>
      </div>
      <div className="ech-info">
        L'échéancier (dates et montants des échéances) est saisi et validé dans PROASSUR. Une fois
        l'accord validé, il s'affiche en lecture seule dans l'onglet « Mise à jour DPD ». Aucune
        saisie d'échéance dans le module.
      </div>

      {/* Commentaire */}
      <div className="sec-head"><h3>Commentaire à l'attention du valideur</h3></div>
      <div className="grid">
        <div className="field full">
          <textarea value={commentaire} onChange={(e) => setCommentaire(e.target.value)} placeholder="Motif, précisions…" />
        </div>
      </div>

      {/* Pièces */}
      <div className="sec-head">
        <h3>Registre de Commerce (RC)</h3>
        <span className="ro-tag">pièce obligatoire</span>
      </div>
      <PieceCapture
        label="Registre de Commerce"
        accent
        obligatoire
        fichiers={rc}
        onChange={setRc}
        hint="Pièce essentielle de la demande (PDF / scan). Plusieurs pages possibles."
      />

      <div className="sec-head">
        <h3>Autres pièces jointes</h3>
        <span className="ro-tag">via GED OneBase</span>
      </div>
      <PieceCapture label="Autres pièces" fichiers={autres} onChange={setAutres} hint="Facultatif." />

      <div className="form-actions">
        <button className="btn-primary" disabled={envoi || !peutEnvoyer} onClick={() => soumettre('envoyer')}>
          ➤ Envoyer pour validation
        </button>
        <button className="btn-ghost" disabled={envoi || !souscription} onClick={() => soumettre('brouillon')}>
          Enregistrer en brouillon
        </button>
        {!peutEnvoyer && souscription && rc.length === 0 && (
          <span className="act-txt">Le RC est obligatoire pour envoyer.</span>
        )}
      </div>

      {message && <p className={`form-msg ${message.type}`}>{message.texte}</p>}
    </div>
  );
}

function Ro({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="field">
      <label>{label}</label>
      <input className="ro" readOnly value={value ?? ''} placeholder="—" />
    </div>
  );
}
