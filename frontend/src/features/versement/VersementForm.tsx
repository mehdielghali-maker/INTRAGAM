import { useState } from 'react';
import { DeposerVersementRequest, SituationMois, VersementOptions } from './types';
import { FichierLocal } from './imageCompression';
import { enregistrerBrouillon, soumettre } from './api';
import { dateDuJourIso } from './format';
import PieceCapture from './PieceCapture';
import SituationMoisPanel from './SituationMoisPanel';

/**
 * Formulaire de dépôt d'un versement bancaire. Sélecteur de mois (clé), situation du mois
 * (lecture seule), capture du reçu (obligatoire), montant/date/référence/banque, commentaire.
 * « Soumettre au BPM » désactivé tant qu'aucun reçu n'est joint (le back l'impose aussi).
 */
export default function VersementForm({
  options,
  situation,
  situationErreur,
  mois,
  onMoisChange,
  onSuccess,
}: {
  options: VersementOptions | null;
  situation: SituationMois | null;
  situationErreur: string | null;
  mois: string;
  onMoisChange: (mois: string) => void;
  onSuccess: () => void;
}) {
  const [montant, setMontant] = useState('');
  const [dateVersement, setDateVersement] = useState(dateDuJourIso());
  const [referenceBordereau, setReferenceBordereau] = useState('');
  const [banque, setBanque] = useState('');
  const [commentaire, setCommentaire] = useState('');
  const [pieces, setPieces] = useState<FichierLocal[]>([]);

  const [envoi, setEnvoi] = useState(false);
  const [message, setMessage] = useState<{ type: 'ok' | 'ko'; texte: string } | null>(null);

  function reinitialiser() {
    setMontant('');
    setDateVersement(dateDuJourIso());
    setReferenceBordereau('');
    setBanque('');
    setCommentaire('');
    setPieces([]);
  }

  function payload(): DeposerVersementRequest {
    const montantNum = montant.trim() ? Number(montant.replace(/\s/g, '').replace(',', '.')) : null;
    return {
      moisSituation: mois,
      montantVerse: montantNum != null && !Number.isNaN(montantNum) ? montantNum : null,
      dateVersement,
      referenceBordereau: referenceBordereau.trim() || undefined,
      banque: banque.trim() || undefined,
      commentaire: commentaire.trim() || undefined,
      nomsFichiers: pieces.map((f) => f.name),
    };
  }

  const peutSoumettre = pieces.length > 0; // reçu obligatoire (back l'impose aussi)

  async function action(type: 'soumettre' | 'brouillon') {
    setEnvoi(true);
    setMessage(null);
    try {
      const fn = type === 'soumettre' ? soumettre : enregistrerBrouillon;
      const v = await fn(payload());
      if (type === 'soumettre') {
        setMessage({
          type: 'ok',
          texte: `Versement ${v.reference ?? ''} soumis au BPM (${v.statutLibelle}).`.replace('  ', ' '),
        });
      } else {
        setMessage({ type: 'ok', texte: 'Brouillon enregistré.' });
      }
      reinitialiser();
      onSuccess();
    } catch (e) {
      setMessage({ type: 'ko', texte: e instanceof Error ? e.message : 'Erreur' });
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <div className="form-card">
      {/* MOIS */}
      <div className="sec-head">
        <span className="tile" aria-hidden>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
            <rect x="4" y="5" width="16" height="16" rx="2" />
            <path d="M4 9h16M8 3v4M16 3v4" />
          </svg>
        </span>
        <h3>Mois concerné</h3>
      </div>
      <div className="grid2">
        <div className="field key">
          <label htmlFor="vers-mois">
            Mois de la situation financière <span className="req">*</span>
          </label>
          <select id="vers-mois" value={mois} onChange={(e) => onMoisChange(e.target.value)}>
            {options?.mois.map((m) => (
              <option key={m.valeur} value={m.valeur}>
                {m.libelle}
              </option>
            ))}
          </select>
          <span className="hint">Mois des primes émises à justifier</span>
        </div>
      </div>

      {/* SITUATION DU MOIS */}
      <SituationMoisPanel situation={situation} erreur={situationErreur} />

      {/* REÇU */}
      <div className="sec-head">
        <span className="tile" aria-hidden>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
            <rect x="5" y="3" width="14" height="18" rx="2" />
            <path d="M9 8h6M9 12h6M9 16h3" />
          </svg>
        </span>
        <h3>Reçu de versement</h3>
        <span className="ro-tag obligatoire">pièce obligatoire</span>
      </div>
      <PieceCapture
        titre="Joindre le reçu de versement bancaire"
        sousTitre="Prenez une photo du bordereau ou ajoutez un fichier (plusieurs pages possibles — PDF / image)"
        obligatoire
        fichiers={pieces}
        onChange={setPieces}
      />

      {/* CHAMPS */}
      <div className="grid4" style={{ marginTop: 16 }}>
        <div className="field">
          <label htmlFor="vers-montant">
            Montant versé (DA) <span className="req">*</span>
          </label>
          <input
            id="vers-montant"
            inputMode="numeric"
            value={montant}
            onChange={(e) => setMontant(e.target.value)}
            placeholder="Ex. 1 450 000"
          />
        </div>
        <div className="field">
          <label htmlFor="vers-date">
            Date du versement <span className="req">*</span>
          </label>
          <input
            id="vers-date"
            type="date"
            value={dateVersement}
            onChange={(e) => setDateVersement(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="vers-ref">Référence bordereau</label>
          <input
            id="vers-ref"
            value={referenceBordereau}
            onChange={(e) => setReferenceBordereau(e.target.value)}
            placeholder="N° bordereau / bP"
          />
        </div>
        <div className="field">
          <label htmlFor="vers-banque">Banque</label>
          <select id="vers-banque" value={banque} onChange={(e) => setBanque(e.target.value)}>
            <option value="">—</option>
            {options?.banques.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="field full" style={{ marginTop: 14 }}>
        <label htmlFor="vers-commentaire">
          Commentaire <span className="opt">— facultatif</span>
        </label>
        <textarea
          id="vers-commentaire"
          value={commentaire}
          onChange={(e) => setCommentaire(e.target.value)}
          placeholder="Précisions sur le versement…"
        />
      </div>

      <div className="form-actions">
        <button
          className="btn-primary"
          disabled={envoi || !peutSoumettre}
          onClick={() => action('soumettre')}
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
            <path d="M22 2 11 13M22 2l-7 20-4-9-9-4z" />
          </svg>
          Soumettre au BPM
        </button>
        <button className="btn-ghost" disabled={envoi} onClick={() => action('brouillon')}>
          Enregistrer en brouillon
        </button>
        {!peutSoumettre && (
          <span className="act-txt">Le reçu de versement est obligatoire pour soumettre.</span>
        )}
      </div>

      {message && <p className={`form-msg ${message.type}`}>{message.texte}</p>}
    </div>
  );
}
