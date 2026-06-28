import { useState } from 'react';
import { Catalogue, definition, PieceCapturee, piecesDuGroupe } from './catalogue';
import Lightbox from './Lightbox';
import './sinistre-ui.css';

const CHECK = 'M5 12l5 5L20 6';
const ZOOM = 'M11 8v6M8 11h6M21 21l-4.3-4.3M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z';
const WARN = 'M10.3 3.9l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01';

/**
 * Écran de contrôle partagé (détail AGA + récap client), piloté par le CATALOGUE : bandeau de
 * complétude (règle CALCULÉE via catalogue.piecesManquantes) + vignettes groupées (présente →
 * lightbox ; obligatoire manquante → « Manquant » ; facultative → « Non fourni »). `resolveUrl`
 * fournit l'URL d'aperçu (dataUrl côté AGA, objectURL du blob Dexie côté client).
 */
export default function ApercusControle({
  catalogue,
  contexte,
  pieces,
  resolveUrl,
}: {
  catalogue: Catalogue;
  contexte: unknown;
  pieces: PieceCapturee[];
  resolveUrl?: (piece: PieceCapturee) => string | undefined;
}) {
  const [zoom, setZoom] = useState<string | null>(null);

  const present = (type: string) => pieces.find((p) => p.type === type);
  const manquantes = catalogue.piecesManquantes(pieces, contexte);
  const complet = manquantes.length === 0;
  const url = (p: PieceCapturee) => (resolveUrl ? resolveUrl(p) : p.dataUrl);

  return (
    <div>
      <div className={`sui-cb ${complet ? 'ok' : 'ko'}`}>
        <span className="cb-ic">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
            <path d={complet ? CHECK : WARN} />
          </svg>
        </span>
        <div>
          <div className="cb-t">{complet ? 'Dossier complet — prêt à valider' : 'Dossier incomplet'}</div>
          <div className="cb-s">
            {complet
              ? 'Toutes les pièces obligatoires sont présentes.'
              : `Pièces obligatoires manquantes : ${manquantes.map((t) => definition(catalogue, t)?.libelle ?? t).join(', ')}.`}
          </div>
        </div>
      </div>

      {catalogue.groupes.map((g) => (
        <div key={g.id}>
          <div className="sui-glabel">{g.libelle}</div>
          <div className="sui-thumbs">
            {piecesDuGroupe(catalogue, g.id).map((def) => {
              const p = present(def.type);
              if (p) {
                const src = url(p);
                return (
                  <div key={def.type} className="sui-thumb" onClick={() => src && setZoom(src)}>
                    <div className="img">
                      {p.estPdf || !src ? <span>PDF</span> : <img src={src} alt={def.libelle} />}
                      <span className="zoom"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={ZOOM} /></svg></span>
                    </div>
                    <div className="cap">
                      <span className="t">{def.libelle}</span>
                      <span className="ok"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.4}><path d={CHECK} /></svg></span>
                    </div>
                  </div>
                );
              }
              const manquanteReq = manquantes.includes(def.type);
              return (
                <div key={def.type} className={`sui-thumb ${manquanteReq ? 'miss-req' : 'miss-opt'}`}>
                  <div className="img">{manquanteReq ? 'Manquant' : 'Non fourni'}</div>
                  <div className="cap"><span className="t">{def.libelle}{manquanteReq ? ' *' : ''}</span></div>
                </div>
              );
            })}
          </div>
        </div>
      ))}

      {zoom && <Lightbox src={zoom} onClose={() => setZoom(null)} />}
    </div>
  );
}
