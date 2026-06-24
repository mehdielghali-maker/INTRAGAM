import { useEffect, useState } from 'react';
import { consulterContexte, enregistrerBrouillon, envoyerDemande } from './api';
import { ContexteCotation } from './types';

export default function NouvelleDemandeTab({ onEnvoye }: { onEnvoye: () => void }) {
  const [ctx, setCtx] = useState<ContexteCotation | null>(null);
  const [objet, setObjet] = useState('');
  const [nomProspect, setNomProspect] = useState('');
  const [numeroPolice, setNumeroPolice] = useState('');
  const [commentaire, setCommentaire] = useState('');
  const [fichiers, setFichiers] = useState<string[]>([]);
  const [erreurs, setErreurs] = useState<{ objet?: string; nomProspect?: string }>({});
  const [message, setMessage] = useState<{ type: 'ok' | 'ko'; texte: string } | null>(null);
  const [envoi, setEnvoi] = useState(false);

  useEffect(() => {
    consulterContexte()
      .then(setCtx)
      .catch((e) => setMessage({ type: 'ko', texte: e instanceof Error ? e.message : 'Erreur' }));
  }, []);

  function valider(): boolean {
    const errs: { objet?: string; nomProspect?: string } = {};
    if (!objet) errs.objet = 'Sélectionnez une branche / objet';
    if (!nomProspect.trim()) errs.nomProspect = 'Le nom du prospect est obligatoire';
    setErreurs(errs);
    return Object.keys(errs).length === 0;
  }

  function payload() {
    return {
      objet,
      nomProspect: nomProspect.trim(),
      numeroPolice: numeroPolice.trim() || undefined,
      commentaire: commentaire.trim() || undefined,
      nomsFichiers: fichiers,
    };
  }

  async function soumettre(action: 'envoyer' | 'brouillon') {
    if (action === 'envoyer' && !valider()) return;
    setEnvoi(true);
    setMessage(null);
    try {
      const fn = action === 'envoyer' ? envoyerDemande : enregistrerBrouillon;
      const d = await fn(payload());
      if (action === 'envoyer') {
        setMessage({ type: 'ok', texte: `Demande ${d.reference} envoyée au central.` });
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

  function ajouterFichiers(liste: FileList | null) {
    if (!liste) return;
    setFichiers((prev) => [...prev, ...Array.from(liste).map((f) => f.name)]);
  }

  return (
    <div className="form-card">
      <div className="sso-strip">
        <span className="ms" aria-hidden>
          {/* logo Microsoft simplifié */}
          <svg viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" /><rect x="13" y="3" width="8" height="8" /><rect x="3" y="13" width="8" height="8" /><rect x="13" y="13" width="8" height="8" /></svg>
        </span>
        <div className="who">
          <b>Demande créée par {ctx?.identite.utilisateur ?? '…'}</b>
          <span>
            Connecté via SSO Microsoft · {ctx?.identite.nomAgence} ({ctx?.identite.codeAgence})
          </span>
        </div>
      </div>

      <div className="grid2">
        <div className="field">
          <label>N° Demande</label>
          <input className="ro" readOnly value="Auto-généré à l'envoi" />
        </div>
        <div className="field">
          <label>
            N° Police <span className="opt">— optionnel (renouvellement / avenant)</span>
          </label>
          <input
            value={numeroPolice}
            onChange={(e) => setNumeroPolice(e.target.value)}
            placeholder="Vide pour une affaire nouvelle"
          />
        </div>

        <div className="field">
          <label>Branche / Objet</label>
          <select value={objet} onChange={(e) => setObjet(e.target.value)}>
            <option value="">— Sélectionner —</option>
            {ctx?.branches.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
          {erreurs.objet && <span className="err">{erreurs.objet}</span>}
        </div>
        <div className="field">
          <label>Nom / Raison sociale du prospect</label>
          <input value={nomProspect} onChange={(e) => setNomProspect(e.target.value)} />
          {erreurs.nomProspect && <span className="err">{erreurs.nomProspect}</span>}
        </div>

        <div className="field">
          <label>Code Agence</label>
          <input className="ro" readOnly value={ctx?.identite.codeAgence ?? ''} />
          <span className="hint">Pré-rempli depuis votre session</span>
        </div>
        <div className="field">
          <label>Direction Régionale</label>
          <input className="ro" readOnly value={ctx?.identite.directionRegionale ?? ''} />
          <span className="hint">Déduite de votre agence</span>
        </div>

        <div className="field full">
          <label>Commentaire à l'attention du central</label>
          <textarea
            value={commentaire}
            onChange={(e) => setCommentaire(e.target.value)}
            placeholder="Motif, précisions, contexte…"
          />
        </div>

        <div className="field full">
          <label>
            Pièces jointes <span className="opt">— via GED OneBase</span>
          </label>
          <div className="attach-zone">
            <input type="file" multiple onChange={(e) => ajouterFichiers(e.target.files)} />
            {fichiers.length > 0 && (
              <div className="attach-list">
                {fichiers.map((f, i) => (
                  <span className="chip-file" key={`${f}-${i}`}>
                    📎 {f}
                    <button
                      type="button"
                      aria-label={`Retirer ${f}`}
                      onClick={() => setFichiers((prev) => prev.filter((_, j) => j !== i))}
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}
            <span className="hint">Les fichiers sont déposés dans la GED OneBase ; le poste n'en conserve que la référence.</span>
          </div>
        </div>
      </div>

      <div className="form-actions">
        <button className="btn-primary" disabled={envoi} onClick={() => soumettre('envoyer')}>
          ➤ Envoyer la demande
        </button>
        <button className="btn-ghost" disabled={envoi} onClick={() => soumettre('brouillon')}>
          Enregistrer en brouillon
        </button>
      </div>

      {message && <p className={`form-msg ${message.type}`}>{message.texte}</p>}
    </div>
  );
}
