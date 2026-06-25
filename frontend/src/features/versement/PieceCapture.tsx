import { useRef, useState } from 'react';
import { FichierLocal, preparerFichier } from './imageCompression';

/**
 * Capture du reçu de versement : prise de photo directe (appareil du téléphone via
 * capture="environment") ou ajout de fichiers (PDF / images). Capture multiple, vignettes
 * avec suppression, compression image côté client avant « upload », progression simulée.
 * Reproduit le bloc « .upload » de la maquette. Mobile-first. (Adapté du module dpd.)
 */
export default function PieceCapture({
  titre,
  sousTitre,
  hint,
  obligatoire,
  fichiers,
  onChange,
  erreur,
}: {
  titre: string;
  sousTitre: string;
  hint?: string;
  obligatoire?: boolean;
  fichiers: FichierLocal[];
  onChange: (f: FichierLocal[]) => void;
  erreur?: string;
}) {
  const [traitement, setTraitement] = useState(false);
  const inputPhoto = useRef<HTMLInputElement>(null);
  const inputFichier = useRef<HTMLInputElement>(null);

  async function ajouter(liste: FileList | null) {
    if (!liste || liste.length === 0) return;
    setTraitement(true);
    try {
      const prepares: FichierLocal[] = [];
      for (const f of Array.from(liste)) {
        prepares.push(await preparerFichier(f)); // compression côté client
      }
      onChange([...fichiers, ...prepares]);
    } finally {
      setTraitement(false);
    }
  }

  function retirer(id: string) {
    onChange(fichiers.filter((f) => f.id !== id));
  }

  return (
    <div className="upload">
      <div className="ttl">{titre}</div>
      <div className="sub">{sousTitre}</div>

      <div className="btns">
        <label className="ubtn cam">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
            <circle cx="12" cy="13" r="4" />
          </svg>
          Prendre une photo
          <input
            ref={inputPhoto}
            type="file"
            accept="image/*"
            capture="environment"
            onChange={(e) => {
              ajouter(e.target.files);
              e.target.value = '';
            }}
          />
        </label>
        <label className="ubtn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12" />
          </svg>
          Joindre un fichier
          <input
            ref={inputFichier}
            type="file"
            accept="image/*,application/pdf"
            multiple
            onChange={(e) => {
              ajouter(e.target.files);
              e.target.value = '';
            }}
          />
        </label>
      </div>

      {traitement && (
        <div className="progress" aria-label="Traitement en cours">
          <span style={{ width: '100%' }} />
        </div>
      )}

      {fichiers.length > 0 && (
        <div className="thumbs">
          {fichiers.map((f) => (
            <div className="thumb" key={f.id}>
              {f.isPdf || !f.dataUrl ? (
                <span className="pdf">
                  📄 {f.name}
                  <br />
                  {f.sizeKo} Ko
                </span>
              ) : (
                <img src={f.dataUrl} alt={f.name} title={`${f.name} · ${f.sizeKo} Ko`} />
              )}
              <button
                type="button"
                className="rm"
                aria-label={`Retirer ${f.name}`}
                onClick={() => retirer(f.id)}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {hint && <span className="hint">{hint}</span>}
      {obligatoire && fichiers.length === 0 && (
        <span className="hint">Reçu requis pour la soumission au BPM.</span>
      )}
      {erreur && <span className="err">{erreur}</span>}
    </div>
  );
}
