import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { brouillonsLocaux, catalogueDeclaration, decsin, Declaration, peutEnvoyer } from '@decsin';
import { ApercusControle, VerificationPlaque } from '@sinistre-ui';
import StatutDeclarationBadge from './StatutDeclarationBadge';
import './sinistre.css';

/**
 * Détail / contrôle d'une déclaration (AGA). En-tête + complétude + vignettes (lightbox), puis
 * « Valider → PROASSUR » (désactivé si une pièce obligatoire manque) ou « Renvoyer au client ».
 */
export default function DetailControlePage() {
  const { idLocal } = useParams();
  const navigate = useNavigate();
  const [decl, setDecl] = useState<Declaration | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const charger = useCallback(async () => {
    // Un brouillon présentiel vit en local ; les autres dans DECSIN.
    const [brouillons, envoyees] = await Promise.all([brouillonsLocaux.lister(), decsin.getDeclarations()]);
    setDecl([...brouillons, ...envoyees].find((d) => d.idLocal === idLocal) ?? null);
  }, [idLocal]);

  useEffect(() => {
    void charger();
  }, [charger]);

  if (!decl) {
    return (
      <section className="sinistre">
        <button className="retour" onClick={() => navigate('/declaration-sinistre')}>← Retour au suivi</button>
        <p className="vide">Déclaration introuvable.</p>
      </section>
    );
  }

  const tiers = Boolean(decl.compagnieAdverse?.trim() || decl.vehiculeAdverse?.trim());
  const complet = peutEnvoyer(decl);

  async function valider() {
    if (!decl) return;
    const { numSinistre, idDossierSinistre } = await decsin.rattacherDeclaration(decl);
    await decsin.creerDeclaration({ ...decl, statut: 'VALIDEE', numSinistre, idDossierSinistre });
    setMessage(`Rattachée à PROASSUR — N° sinistre ${numSinistre}.`);
    await charger();
  }

  async function renvoyer() {
    if (!decl) return;
    await decsin.creerDeclaration({ ...decl, statut: 'RELANCE' });
    setMessage('Renvoyée au client pour correction.');
    await charger();
  }

  return (
    <section className="sinistre">
      <button className="retour" onClick={() => navigate('/declaration-sinistre')}>← Retour au suivi</button>
      {message && <p style={{ color: 'var(--vert)', fontWeight: 600, marginBottom: 12 }}>{message}</p>}

      <div className="detail-head">
        <div><div className="dh-k">Code</div><div className="dh-v">{decl.code}</div></div>
        <div><div className="dh-k">Véhicule</div><div className="dh-v">{decl.marque} · {decl.immatriculation}</div></div>
        <div><div className="dh-k">Conducteur / client</div><div className="dh-v">{decl.client?.nom ?? decl.conducteur ?? '—'}</div></div>
        <div><div className="dh-k">Date</div><div className="dh-v">{decl.dateSinistre || '—'}</div></div>
        <div><div className="dh-k">Origine</div><div className="dh-v">{decl.origine === 'AGA' ? 'AGA' : 'Client'}</div></div>
        <div><div className="dh-k">Statut</div><StatutDeclarationBadge statut={decl.statut} /></div>
        {decl.numSinistre && <div><div className="dh-k">N° sinistre</div><div className="dh-v">{decl.numSinistre}</div></div>}
      </div>

      {(decl.lieuSinistre || decl.observations) && (
        <div className="form-card" style={{ marginBottom: 16 }}>
          <div className="sec-head"><h3>Informations du sinistre</h3></div>
          <div className="grid2">
            <div className="field"><label>Lieu</label><input className="ro" value={decl.lieuSinistre || '—'} readOnly /></div>
            <div className="field"><label>Heure</label><input className="ro" value={decl.heureSinistre || '—'} readOnly /></div>
            <div className="field full"><label>Circonstances</label><textarea className="ro" value={decl.observations || '—'} readOnly /></div>
          </div>
        </div>
      )}

      <ApercusControle catalogue={catalogueDeclaration} contexte={{ tiers }} pieces={decl.pieces} />

      <VerificationPlaque pieces={decl.pieces} immatriculation={decl.immatriculation} />

      {decl.statut === 'BROUILLON' && (
        <div className="form-actions">
          <button className="btn-primary" onClick={() => navigate(`/declaration-sinistre?reprendre=${encodeURIComponent(decl.idLocal)}`)}>
            Reprendre la saisie
          </button>
          <span className="hint">Brouillon présentiel — non envoyé à PROASSUR tant qu'il n'est pas validé.</span>
        </div>
      )}

      {decl.statut === 'A_VALIDER' && (
        <div className="form-actions">
          <button className="btn-primary" disabled={!complet} onClick={valider}>Valider → PROASSUR</button>
          <button className="btn-ghost" onClick={renvoyer}>Renvoyer au client</button>
          {!complet && <span className="hint">Validation impossible : pièces obligatoires manquantes.</span>}
        </div>
      )}
    </section>
  );
}
