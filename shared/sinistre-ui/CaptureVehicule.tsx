import { useEffect, useRef, useState } from 'react';
import { Catalogue, PieceCapturee, vuesVehicule } from './catalogue';
import { preparerPiece } from './media';

const CHECK = 'M5 12l5 5L20 6';
const CAM = 'M4 8h3l1.5-2h7L17 8h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z';

/**
 * Socle de capture VÉHICULE partagé, piloté par le CATALOGUE : sélecteur de vues (chips) +
 * SILHOUETTE SVG en overlay (catalogue.silhouettes) qui change selon la vue, par-dessus le flux
 * caméra (calque de guidage — la photo n'est PAS rognée). Les vues sans silhouette affichent le
 * cadre seul. Deux sources : caméra ou galerie. Contrôlé par le parent.
 */
export default function CaptureVehicule({
  catalogue,
  pieces,
  onAjouter,
  onSupprimer,
}: {
  catalogue: Catalogue;
  pieces: PieceCapturee[];
  onAjouter: (piece: PieceCapturee, blob: Blob) => void;
  onSupprimer: (type: string) => void;
}) {
  const vues = vuesVehicule(catalogue);
  const [vue, setVue] = useState<string>(vues[0]?.type ?? '');
  const videoRef = useRef<HTMLVideoElement>(null);
  const [cameraKo, setCameraKo] = useState(false);

  useEffect(() => {
    let flux: MediaStream | null = null;
    navigator.mediaDevices
      ?.getUserMedia({ video: { facingMode: 'environment' }, audio: false })
      .then((s) => {
        flux = s;
        if (videoRef.current) {
          videoRef.current.srcObject = s;
        }
      })
      .catch(() => setCameraKo(true));
    return () => flux?.getTracks().forEach((t) => t.stop());
  }, []);

  const prise = (type: string) => pieces.some((p) => p.type === type);
  const def = vues.find((v) => v.type === vue) ?? vues[0];

  function vueSuivante(apres: string) {
    const restantes = vues.filter((v) => v.type !== apres && !prise(v.type));
    if (restantes[0]) {
      setVue(restantes[0].type);
    }
  }

  async function ajouter(file: File, type: string) {
    const { piece, blob } = await preparerPiece(file, type);
    onAjouter(piece, blob);
    vueSuivante(type);
  }

  function capturer() {
    const video = videoRef.current;
    if (!video || !video.videoWidth) {
      return;
    }
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height); // image entière (pas de rognage)
    canvas.toBlob((b) => b && void ajouter(new File([b], `${vue}.jpg`, { type: 'image/jpeg' }), vue), 'image/jpeg', 0.9);
  }

  function galerie() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = () => {
      const f = input.files?.[0];
      if (f) {
        void ajouter(f, vue);
      }
    };
    input.click();
  }

  return (
    <div className="sui-frame">
      <div className="sui-viewfinder">
        <video ref={videoRef} autoPlay playsInline muted />
        <span className="sui-corner sui-tl" />
        <span className="sui-corner sui-tr" />
        <span className="sui-corner sui-bl" />
        <span className="sui-corner sui-br" />
        <span className="sui-tag">Vue : {def?.libelle ?? ''}</span>
        <div className="sui-guide" dangerouslySetInnerHTML={{ __html: catalogue.silhouettes[vue] ?? '' }} />
        {cameraKo && <span className="sui-vf-err">Caméra indisponible — utilisez « Galerie ».</span>}
        <button type="button" className="sui-cam" onClick={capturer} disabled={cameraKo} title="Prendre la photo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><circle cx="12" cy="13" r="4" /><path d={CAM} /></svg>
        </button>
      </div>

      <div className="sui-chips">
        {vues.map((v) => (
          <button
            type="button"
            key={v.type}
            className={`sui-chip ${v.type === vue ? 'active' : ''} ${prise(v.type) ? 'done' : ''}`}
            onClick={() => setVue(v.type)}
          >
            <span className="fi" dangerouslySetInnerHTML={{ __html: catalogue.iconesVue[v.type] ?? '' }} />
            <span className="fl">{v.libelle}{v.obligatoire ? '' : ' (opt.)'}</span>
            <span className="fdone"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.6}><path d={CHECK} /></svg></span>
          </button>
        ))}
      </div>

      <div className="sui-cap">
        Choisissez la vue, cadrez le véhicule puis prenez la photo — ou{' '}
        <span className="sui-link" onClick={galerie}>importez depuis la galerie</span>.
        {prise(vue) && (
          <> · <span className="sui-link" onClick={() => onSupprimer(vue)}>retirer cette vue</span></>
        )}
      </div>
    </div>
  );
}
