import { useEffect, useRef, useState } from 'react';
import { CODE_CONSOLIDE, ContexteAgence } from './agence';

const PIN = 'M3 21h18M5 21V7l7-4 7 4v14M9 21v-5h6v5';
const CHECK = 'M5 12l5 5L20 6';
const CHEVRON = 'M6 9l6 6 6-6';

function PinIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.7} aria-hidden="true">
      <path d={PIN} />
    </svg>
  );
}

/**
 * Barre supérieure : à gauche, logo GAM + identité de l'utilisateur connecté (SSO) ; à
 * droite, le commutateur « Agence active » (remplace l'ancien sélecteur Période/YTD,
 * supprimé). Tous les écrans héritent de l'agence choisie ici.
 */
export default function Topbar({
  contexte,
  onChanger,
}: {
  contexte: ContexteAgence | null;
  onChanger: (code: string) => void;
}) {
  const [ouvert, setOuvert] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function surClicExterne(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOuvert(false);
    }
    document.addEventListener('click', surClicExterne);
    return () => document.removeEventListener('click', surClicExterne);
  }, []);

  const utilisateur = contexte?.utilisateur;
  const active = contexte?.agenceActive;
  const agences = contexte?.agencesAutorisees ?? [];
  const consolide = contexte?.consolideActif ?? false;
  const consolideDispo = contexte?.consolideDisponible ?? false;
  const btnNom = consolide ? 'Toutes mes agences' : (active?.nom ?? 'Agence');
  const btnCode = consolide ? 'Consolidé' : (active?.code ?? '—');
  const estAga = utilisateur?.profil === 'AGA';
  const profilLib = estAga ? 'Agent Général' : 'Agent';
  const n = agences.length;
  const meta = utilisateur
    ? `${estAga ? 'Espace AGA' : 'Espace agence'} · ${n} agence${n > 1 ? 's' : ''} gérée${n > 1 ? 's' : ''} · session ouverte`
    : 'Session en cours…';

  function choisir(code: string) {
    setOuvert(false);
    if (code !== active?.code) onChanger(code);
  }

  return (
    <header className="topbar">
      <div className="brandblock">
        <span className="logo-tile">
          <img src="/logo-gam.webp" alt="GAM Assurances" />
        </span>
        <div>
          <div className="agence-nom">
            {utilisateur ? `${utilisateur.nomAffiche} — ${profilLib}` : 'Poste de travail unifié'}
            <span className="maquette-tag">Données fictives</span>
          </div>
          <div className="agence-meta">{meta}</div>
        </div>
      </div>

      <div className="ca-zone">
        <div className="switcher right" ref={ref}>
          <div className="switch-label">Agence active</div>
          <button
            type="button"
            className="switch-btn"
            aria-haspopup="listbox"
            aria-expanded={ouvert}
            onClick={(e) => {
              e.stopPropagation();
              setOuvert((o) => !o);
            }}
            disabled={!contexte}
          >
            <span className="sw-pin">
              <PinIcon />
            </span>
            <span className="sw-txt">
              <span className="sw-ag">{btnNom}</span>
              <span className="sw-aga">{btnCode}</span>
            </span>
            <span className="chev">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.2} aria-hidden="true">
                <path d={CHEVRON} />
              </svg>
            </span>
          </button>

          <div className={`switch-menu ${ouvert ? 'open' : ''}`} role="listbox">
            <div className="sm-h">Changer d'agence</div>
            {agences.map((a) => (
              <button
                type="button"
                key={a.code}
                role="option"
                aria-selected={a.code === active?.code}
                className={`sm-item ${a.code === active?.code ? 'active' : ''}`}
                onClick={() => choisir(a.code)}
              >
                <span className="d">
                  <PinIcon />
                </span>
                <span className="t">{a.nom}</span>
                <span className="ok">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5} aria-hidden="true">
                    <path d={CHECK} />
                  </svg>
                </span>
              </button>
            ))}
            {consolideDispo && (
              <button
                type="button"
                role="option"
                aria-selected={consolide}
                className={`sm-item ${consolide ? 'active' : ''}`}
                onClick={() => {
                  setOuvert(false);
                  if (!consolide) onChanger(CODE_CONSOLIDE);
                }}
                title="Vue d'ensemble en lecture seule (aucune action)"
              >
                <span className="d">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.7} aria-hidden="true">
                    <rect x="3" y="3" width="7" height="7" />
                    <rect x="14" y="3" width="7" height="7" />
                    <rect x="3" y="14" width="7" height="7" />
                    <rect x="14" y="14" width="7" height="7" />
                  </svg>
                </span>
                <span className="t">Toutes mes agences (consolidé)</span>
                <span className="ok">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5} aria-hidden="true">
                    <path d={CHECK} />
                  </svg>
                </span>
              </button>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
