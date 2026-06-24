import { useRef, useState } from 'react';
import { FichierLocal, preparerFichier } from './imageCompression';

/**
 * Contrôle de pièces jointes : ajout de fichiers (PDF/images) ET prise de photo directe
 * (appareil du téléphone via capture="environment"). Capture multiple, vignettes avec
 * suppression, compression image avant « upload », progression. Mobile-first.
 */
export default function PieceCapture({
  label,
  hint,
  obligatoire,
  accent,
  fichiers,
  onChange,
  erreur,
}: {
  label: string;
  hint?: string;
  obligatoire?: boolean;
  accent?: boolean;
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
    <div className={`attach ${accent ? 'rc' : ''}`}>
      <div className="attach-buttons">
        <label className="attach-btn">
          📷 Prendre une photo
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
        <label className="attach-btn">
          📎 Ajouter un fichier
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
              <button type="button" className="rm" aria-label={`Retirer ${f.name}`} onClick={() => retirer(f.id)}>
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {hint && <span className="hint">{hint}</span>}
      {obligatoire && fichiers.length === 0 && (
        <span className="hint">{label} requis pour l'envoi.</span>
      )}
      {erreur && <span className="err">{erreur}</span>}
    </div>
  );
}
