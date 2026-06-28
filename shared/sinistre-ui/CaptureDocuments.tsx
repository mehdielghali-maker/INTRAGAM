import { Catalogue, PieceCapturee, piecesDuGroupe } from './catalogue';
import { preparerPiece } from './media';

const CAM = 'M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z';
const CHECK = 'M5 12l5 5L20 6';

function IcoCam() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <path d={CAM} /><circle cx="12" cy="13" r="4" />
    </svg>
  );
}

/**
 * Capture d'un groupe de pièces SANS masque (constat, documents, identité…), en grille ou liste.
 * Deux sources : « Prendre » (caméra native) ou « Galerie ». Compression via preparerPiece.
 * SEAM OCR : `onAjouter` est le point de branchement d'un futur service OCR (pré-remplissage
 * des champs depuis le document scanné) — NON implémenté ici.
 */
export default function CaptureDocuments({
  catalogue,
  groupe,
  pieces,
  estObligatoire,
  layout = 'grille',
  onAjouter,
  onSupprimer,
}: {
  catalogue: Catalogue;
  groupe: string;
  pieces: PieceCapturee[];
  estObligatoire: (type: string) => boolean;
  layout?: 'grille' | 'liste';
  onAjouter: (piece: PieceCapturee, blob: Blob) => void;
  onSupprimer: (type: string) => void;
}) {
  async function ajouter(file: File, type: string) {
    const { piece, blob } = await preparerPiece(file, type);
    onAjouter(piece, blob);
  }

  function choisir(type: string, source: 'camera' | 'galerie') {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*,application/pdf';
    if (source === 'camera') {
      input.setAttribute('capture', 'environment');
    }
    input.onchange = () => {
      const f = input.files?.[0];
      if (f) {
        void ajouter(f, type);
      }
    };
    input.click();
  }

  return (
    <div className={layout === 'liste' ? 'sui-list' : 'sui-grid'}>
      {piecesDuGroupe(catalogue, groupe).map((def) => {
        const piece = pieces.find((p) => p.type === def.type);
        const obligatoire = estObligatoire(def.type);
        return (
          <div key={def.type} className={`sui-tile ${piece ? 'done' : obligatoire ? 'req' : ''}`}>
            {piece && (
              <span className="ok"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={3}><path d={CHECK} /></svg></span>
            )}
            <div className="ic">
              {piece && !piece.estPdf ? <img src={piece.dataUrl} alt={def.libelle} /> : <IcoCam />}
            </div>
            <div className="txt">
              <div className="lbl">{def.libelle}{obligatoire ? ' *' : ''}</div>
              <div className="sub">{piece ? (piece.estPdf ? 'PDF' : `${piece.tailleKo} Ko`) : def.sousTitre}</div>
            </div>
            <div className="sui-acts">
              {piece ? (
                <button type="button" className="sup" onClick={() => onSupprimer(def.type)}>Supprimer</button>
              ) : (
                <>
                  <button type="button" onClick={() => choisir(def.type, 'camera')}>Prendre</button>
                  <button type="button" onClick={() => choisir(def.type, 'galerie')}>Galerie</button>
                </>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
