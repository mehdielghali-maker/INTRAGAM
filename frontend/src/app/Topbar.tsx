import { Fragment, useEffect, useRef, useState } from 'react';
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

function GridIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.7} aria-hidden="true">
      <rect x="3" y="3" width="7" height="7" />
      <rect x="14" y="3" width="7" height="7" />
      <rect x="3" y="14" width="7" height="7" />
      <rect x="14" y="14" width="7" height="7" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <span className="ok">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5} aria-hidden="true">
        <path d={CHECK} />
      </svg>
    </span>
  );
}

/**
 * Barre supérieure : à gauche, logo GAM + identité de l'utilisateur connecté (SSO) ; à
 * droite, le commutateur « Agence active » hiérarchique (agences, sous-agences, et vues
 * consolidées). Tous les écrans héritent de la sélection faite ici.
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
  const perimetre = contexte?.perimetre ?? [];
  const selection = contexte?.selectionCode;
  const consolideDispo = contexte?.consolideDisponible ?? false;
  const btnNom = contexte?.selectionLibelle ?? 'Agence';
  const btnCode = contexte?.selectionCode ?? '—';
  const estAga = utilisateur?.profil === 'AGA';
  const profilLib = estAga ? 'Agent Général' : 'Agent';
  const nbFeuilles = perimetre.reduce((n, g) => n + (g.sousAgences.length || 1), 0);
  const meta = utilisateur
    ? `${estAga ? 'Espace AGA' : 'Espace agence'} · ${nbFeuilles} agence${nbFeuilles > 1 ? 's' : ''} gérée${nbFeuilles > 1 ? 's' : ''} · session ouverte`
    : 'Session en cours…';

  function choisir(code: string) {
    setOuvert(false);
    if (code !== selection) onChanger(code);
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

            {perimetre.map((g) => {
              const estGroupe = g.sousAgences.length > 0;
              if (!estGroupe) {
                return (
                  <button
                    type="button"
                    key={g.code}
                    role="option"
                    aria-selected={selection === g.code}
                    className={`sm-item ${selection === g.code ? 'active' : ''}`}
                    onClick={() => choisir(g.code)}
                  >
                    <span className="d">
                      <PinIcon />
                    </span>
                    <span className="t">{g.nom}</span>
                    <CheckIcon />
                  </button>
                );
              }
              return (
                <Fragment key={g.code}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={selection === g.code}
                    className={`sm-item sm-groupe ${selection === g.code ? 'active' : ''}`}
                    onClick={() => choisir(g.code)}
                    title="Vue consolidée du groupe (lecture seule)"
                  >
                    <span className="d">
                      <GridIcon />
                    </span>
                    <span className="t">
                      {g.nom} <span className="sm-tag">consolidé</span>
                    </span>
                    <CheckIcon />
                  </button>
                  {g.sousAgences.map((s) => (
                    <button
                      type="button"
                      key={s.code}
                      role="option"
                      aria-selected={selection === s.code}
                      className={`sm-item sm-sous ${selection === s.code ? 'active' : ''}`}
                      onClick={() => choisir(s.code)}
                    >
                      <span className="d">
                        <PinIcon />
                      </span>
                      <span className="t">{s.nom}</span>
                      <CheckIcon />
                    </button>
                  ))}
                </Fragment>
              );
            })}

            {consolideDispo && (
              <button
                type="button"
                role="option"
                aria-selected={selection === CODE_CONSOLIDE}
                className={`sm-item ${selection === CODE_CONSOLIDE ? 'active' : ''}`}
                onClick={() => choisir(CODE_CONSOLIDE)}
                title="Vue d'ensemble de toutes les agences (lecture seule)"
              >
                <span className="d">
                  <GridIcon />
                </span>
                <span className="t">Toutes mes agences (consolidé)</span>
                <CheckIcon />
              </button>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
