import { useEffect, useRef, useState } from 'react';
import { Catalogue, PieceCapturee, vuesVehicule } from './catalogue';
import { preparerPiece } from './media';
import { useEstMobile } from './useEstMobile';
import { AnalyseurPlaque, ResultatVerification, analyseurPoste } from './VerificationPlaque';

const CHECK = 'M5 12l5 5L20 6';
const CAM = 'M4 8h3l1.5-2h7L17 8h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z';
const WARN = 'M10.3 3.9l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01';
const INFO = 'M12 16v-5M12 8h.01M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z';

/** Mappe un type de slot (face_avant, veh_arriere, veh_gauche…) vers la vue attendue par le service
 * RECO. La détection « est-ce un véhicule ? » est indépendante de la vue ; seul avant/arrière lit une
 * plaque. */
function vueReco(type: string): string {
  if (type.endsWith('avant')) return 'avant';
  if (type.endsWith('arriere')) return 'arriere';
  return 'autre';
}

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
  analyser = analyseurPoste,
}: {
  catalogue: Catalogue;
  pieces: PieceCapturee[];
  onAjouter: (piece: PieceCapturee, blob: Blob) => void;
  onSupprimer: (type: string) => void;
  /** Reconnaissance « est-ce un véhicule ? » lancée à la capture (défaut = backend poste). */
  analyser?: AnalyseurPlaque;
}) {
  const vues = vuesVehicule(catalogue);
  const mobile = useEstMobile(); // caméra seulement sur téléphone/tablette ; PC = import de fichier
  const [vue, setVue] = useState<string>(vues[0]?.type ?? '');
  const videoRef = useRef<HTMLVideoElement>(null);
  const [cameraKo, setCameraKo] = useState(false);
  // Résultat RECO par vue capturée : non-véhicule (alerte) / plaque lue (confirmation). Best-effort,
  // jamais bloquant (la pièce est ajoutée avant l'analyse ; panne/hors-ligne = silencieux).
  const [recoParVue, setRecoParVue] = useState<Record<string, ResultatVerification>>({});

  useEffect(() => {
    if (!mobile) return; // PC : pas de webcam, on n'ouvre pas le flux caméra
    let flux: MediaStream | null = null;
    // Résolution BORNÉE (~720p) : sur mobile, un flux/canvas pleine résolution (1080p/4K) sature la
    // mémoire à la capture et fait RECHARGER l'onglet (perte de la saisie). 720p suffit largement.
    navigator.mediaDevices
      ?.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      })
      .then((s) => {
        flux = s;
        if (videoRef.current) {
          videoRef.current.srcObject = s;
        }
      })
      .catch(() => setCameraKo(true));
    return () => flux?.getTracks().forEach((t) => t.stop());
  }, [mobile]);

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
    // Vérification « est-ce un véhicule ? » DÈS la capture — best-effort, JAMAIS bloquante : la pièce
    // est déjà ajoutée ; une panne/refus/hors-ligne du service est avalé silencieusement (pas d'alerte).
    if (analyser) {
      analyser(blob, vueReco(type), '')
        .then((res) => setRecoParVue((m) => ({ ...m, [type]: res })))
        .catch(() => undefined);
    }
  }

  function retirer(type: string) {
    onSupprimer(type);
    setRecoParVue((m) => {
      const n = { ...m };
      delete n[type];
      return n;
    });
  }

  function capturer() {
    const video = videoRef.current;
    if (!video || !video.videoWidth) {
      return;
    }
    try {
      // Canvas BORNÉ à 1280px (mémoire mobile) : on ne crée jamais un canvas pleine résolution.
      const ratio = Math.min(1, 1280 / video.videoWidth);
      const canvas = document.createElement('canvas');
      canvas.width = Math.round(video.videoWidth * ratio);
      canvas.height = Math.round(video.videoHeight * ratio);
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        return;
      }
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      canvas.toBlob((b) => {
        if (b) void ajouter(new File([b], `${vue}.jpg`, { type: 'image/jpeg' }), vue);
        canvas.width = canvas.height = 0; // libère immédiatement la mémoire du canvas (mobile)
      }, 'image/jpeg', 0.9);
    } catch {
      /* échec de capture : on n'interrompt JAMAIS le parcours (pas de crash) */
    }
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
      <div className={`sui-viewfinder ${mobile ? '' : 'pc'}`}>
        {mobile && <video ref={videoRef} autoPlay playsInline muted />}
        <span className="sui-corner sui-tl" />
        <span className="sui-corner sui-tr" />
        <span className="sui-corner sui-bl" />
        <span className="sui-corner sui-br" />
        <span className="sui-tag">Vue : {def?.libelle ?? ''}</span>
        <div className="sui-guide" dangerouslySetInnerHTML={{ __html: catalogue.silhouettes[vue] ?? '' }} />
        {mobile ? (
          <>
            {cameraKo && <span className="sui-vf-err">Caméra indisponible — utilisez « Galerie ».</span>}
            <button type="button" className="sui-cam" onClick={capturer} disabled={cameraKo} title="Prendre la photo">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><circle cx="12" cy="13" r="4" /><path d={CAM} /></svg>
            </button>
          </>
        ) : (
          // Sur ordinateur : pas de webcam, on importe un fichier (la photo vient du téléphone).
          <button type="button" className="sui-import-pc" onClick={galerie}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><circle cx="12" cy="13" r="4" /><path d={CAM} /></svg>
            Choisir une photo
          </button>
        )}
      </div>

      {vues.map((v) => {
        const r = recoParVue[v.type];
        if (!r) return null;
        let variante: 'ok' | 'ko' | 'neutre' | null = null;
        let icone = INFO;
        let texte = '';
        if (r.estVehicule === false) {
          variante = 'ko';
          icone = WARN;
          texte = 'cette photo ne semble pas montrer un véhicule — reprenez la prise de vue.';
        } else if (r.plaqueLue) {
          variante = 'ok';
          icone = CHECK;
          texte = `véhicule détecté · plaque lue « ${r.plaqueLue} ».`;
        } else if (vueReco(v.type) === 'avant' || vueReco(v.type) === 'arriere') {
          variante = 'neutre';
          icone = INFO;
          texte = 'véhicule détecté, mais plaque non lue — rapprochez-vous puis reprenez la photo.';
        }
        if (!variante) return null; // véhicule sur une vue sans plaque attendue → rien à signaler
        return (
          <div key={v.type} className={`sui-reco ${variante}`} role={variante === 'ko' ? 'alert' : undefined}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={icone} /></svg>
            <span><b>{v.libelle}</b> : {texte}</span>
          </div>
        );
      })}

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
        {mobile ? (
          <>
            Choisissez la vue, cadrez le véhicule puis prenez la photo — ou{' '}
            <span className="sui-link" onClick={galerie}>importez depuis la galerie</span>.
          </>
        ) : (
          <>
            Choisissez la vue puis{' '}
            <span className="sui-link" onClick={galerie}>importez la photo</span> (prise depuis un téléphone).
          </>
        )}
        {prise(vue) && (
          <> · <span className="sui-link" onClick={() => retirer(vue)}>retirer cette vue</span></>
        )}
      </div>
    </div>
  );
}
