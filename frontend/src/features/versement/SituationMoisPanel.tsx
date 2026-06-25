import { SituationMois } from './types';
import { montantDA } from './format';

/**
 * Bloc lecture seule « Situation du mois » : 4 tuiles de contexte (Production émise,
 * Encaissé, Déjà versé en banque, Reste à régulariser). Émis/encaissé lus dans PROASSUR,
 * versé dans Sage — le module ne recalcule pas la comptabilité.
 */
export default function SituationMoisPanel({
  situation,
  erreur,
}: {
  situation: SituationMois | null;
  erreur?: string | null;
}) {
  return (
    <>
      <div className="sec-head">
        <span className="tile" aria-hidden>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
            <path d="M4 19V5M4 19h16M8 16v-4M12 16V9M16 16v-6" />
          </svg>
        </span>
        <h3>Situation du mois</h3>
        <span className="ro-tag">PROASSUR (émis / encaissé) · Sage (versé)</span>
      </div>

      {erreur && <p className="form-msg ko">{erreur}</p>}

      <div className="ctx">
        <div className="ctx-tile">
          <div className="l">Production émise</div>
          <div className="v">{situation ? montantDA(situation.productionEmise) : '—'}</div>
          <div className="s">{situation ? `${situation.moisLibelle} · réf. PROASSUR` : 'réf. PROASSUR'}</div>
        </div>
        <div className="ctx-tile">
          <div className="l">Encaissé</div>
          <div className="v">{situation ? montantDA(situation.encaisse) : '—'}</div>
          <div className="s">primes émises ce mois</div>
        </div>
        <div className="ctx-tile">
          <div className="l">Déjà versé en banque</div>
          <div className="v">{situation ? montantDA(situation.dejaVerse) : '—'}</div>
          <div className="s">justifié · réf. Sage</div>
        </div>
        <div className={`ctx-tile ${situation?.aRegulariser ? 'alert' : ''}`}>
          <div className="l">Reste à régulariser</div>
          <div className="v">{situation ? montantDA(situation.resteARegulariser) : '—'}</div>
          <div className="s">encaissé non encore versé</div>
        </div>
      </div>
    </>
  );
}
