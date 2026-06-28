import { FormEvent, useState } from 'react';
import { decsin, Declaration, genererCode, identifiant, Vehicule } from '@decsin';
import { useAgence } from '../../app/AgencyContext';
import DeclarationCaptureStepper from './DeclarationCaptureStepper';

type Mode = 'saisie' | 'lien';

const URL_CLIENT = (import.meta.env.VITE_CLIENT_URL as string) || 'http://localhost:5174';

/**
 * Onglet « Nouvelle déclaration ». PRÉSENTIEL par défaut : l'AGA saisit le dossier en agence
 * (avec brouillon possible). L'envoi d'un LIEN au client est l'EXCEPTION (client qui ne peut pas
 * se déplacer). Sait aussi REPRENDRE un brouillon existant (prop `brouillonInitial`).
 */
export default function NouvelleDeclarationTab({
  brouillonInitial,
  onFini,
}: {
  brouillonInitial?: Declaration | null;
  onFini?: () => void;
} = {}) {
  const { contexte } = useAgence();
  const consolide = contexte?.consolideActif ?? false;

  const [immat, setImmat] = useState('');
  const [vehicule, setVehicule] = useState<Vehicule | null>(null);
  const [mode, setMode] = useState<Mode>('saisie');

  const [clientNom, setClientNom] = useState('');
  const [clientTel, setClientTel] = useState('');
  const [clientEmail, setClientEmail] = useState('');
  const [lienGenere, setLienGenere] = useState<{ code: string; url: string } | null>(null);

  const [erreur, setErreur] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function rechercher(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setMessage(null);
    const v = await decsin.prefillVehiculeByImmat(immat.trim());
    if (!v) {
      setErreur('Aucun contrat trouvé pour cette immatriculation.');
      setVehicule(null);
      return;
    }
    setVehicule(v);
  }

  function terminer(msg: string) {
    setMessage(msg);
    setVehicule(null);
    setImmat('');
    onFini?.();
  }

  // --- REPRISE d'un brouillon : ouvre directement le stepper pré-rempli ------------------------
  if (brouillonInitial) {
    const vehiculeBrouillon: Vehicule = {
      id: identifiant('v'),
      immatriculation: brouillonInitial.immatriculation,
      marque: brouillonInitial.marque,
      numPolice: brouillonInitial.numPolice,
      conducteur: brouillonInitial.conducteur,
    };
    return (
      <div className="form-card">
        <div className="sec-head">
          <h3>Reprise du brouillon {brouillonInitial.code}</h3>
          <button type="button" className="btn-ghost" onClick={() => onFini?.()}>Annuler la reprise</button>
        </div>
        <DeclarationCaptureStepper vehicule={vehiculeBrouillon} brouillon={brouillonInitial} onTermine={terminer} />
      </div>
    );
  }

  async function genererLien(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    if (consolide) {
      setErreur('Action impossible en vue consolidée : sélectionnez une agence.');
      return;
    }
    if (!clientNom.trim() || !clientTel.trim() || !vehicule) {
      setErreur('Nom et téléphone du client sont obligatoires.');
      return;
    }
    const code = genererCode();
    const decl: Declaration = {
      idLocal: identifiant('d'), code, origine: 'CLIENT', statut: 'LIEN_ENVOYE',
      immatriculation: vehicule.immatriculation, marque: vehicule.marque, numPolice: vehicule.numPolice,
      conducteur: vehicule.conducteur, dateSinistre: '', heureSinistre: '', lieuSinistre: '',
      observations: '', blesses: false, pieces: [],
      client: { nom: clientNom.trim(), telephone: clientTel.trim(), email: clientEmail.trim() || undefined },
    };
    await decsin.creerDeclaration(decl);
    setLienGenere({ code, url: `${URL_CLIENT}/?code=${encodeURIComponent(code)}` });
  }

  return (
    <div className="form-card">
      {erreur && <p className="bloc-msg" style={{ marginBottom: 12 }}>{erreur}</p>}
      {message && <p style={{ color: 'var(--vert)', marginBottom: 12, fontWeight: 600 }}>{message}</p>}

      <form className="loader" onSubmit={rechercher}>
        <div className="field">
          <label>Immatriculation <span className="req">*</span></label>
          <input value={immat} placeholder="Ex. 09876-116-16" onChange={(e) => setImmat(e.target.value)} />
        </div>
        <button type="submit" className="btn-primary">Rechercher le contrat</button>
        <span className="hint-strong">Véhicule, marque et n° de police se chargent depuis PROASSUR</span>
      </form>

      {vehicule && (
        <>
          <div className="grid3" style={{ marginTop: 14 }}>
            <div className="field"><label>Marque / modèle</label><input className="ro" value={vehicule.marque} readOnly /></div>
            <div className="field"><label>N° de police</label><input className="ro" value={vehicule.numPolice} readOnly /></div>
            <div className="field"><label>Conducteur</label><input className="ro" value={vehicule.conducteur ?? '—'} readOnly /></div>
          </div>

          <div className="sec-head"><h3>Mode de déclaration</h3></div>
          <div className="seg">
            <button type="button" className={mode === 'saisie' ? 'active' : ''} onClick={() => setMode('saisie')}>Je saisis (présentiel)</button>
            <button type="button" className={mode === 'lien' ? 'active' : ''} onClick={() => setMode('lien')}>Envoyer un lien (à distance)</button>
          </div>
          <p className="hint" style={{ marginTop: -2 }}>
            {mode === 'saisie'
              ? 'Cas normal : le client est en agence, vous saisissez et pouvez enregistrer en brouillon.'
              : 'Exception : seulement si le client ne peut pas se déplacer.'}
          </p>

          {mode === 'saisie' ? (
            consolide ? (
              <p className="bloc-msg">Action impossible en vue consolidée : sélectionnez une agence.</p>
            ) : (
              <DeclarationCaptureStepper vehicule={vehicule} onTermine={terminer} />
            )
          ) : (
            <form onSubmit={genererLien}>
              <div className="sec-head"><h3>Identification du client</h3></div>
              <div className="grid3">
                <div className="field"><label>Nom <span className="req">*</span></label><input value={clientNom} onChange={(e) => setClientNom(e.target.value)} /></div>
                <div className="field"><label>Téléphone <span className="req">*</span></label><input value={clientTel} onChange={(e) => setClientTel(e.target.value)} /></div>
                <div className="field"><label>Email <span className="opt">— facultatif</span></label><input value={clientEmail} onChange={(e) => setClientEmail(e.target.value)} /></div>
              </div>
              {!lienGenere ? (
                <div className="form-actions"><button type="submit" className="btn-primary">Générer le lien</button></div>
              ) : (
                <div className="link-box" style={{ marginTop: 16 }}>
                  <div className="code-chip">Code : {lienGenere.code}</div>
                  <div className="link-row">
                    <input className="link-url" value={lienGenere.url} readOnly />
                    <button type="button" className="btn-ghost" onClick={() => navigator.clipboard?.writeText(lienGenere.url)}>Copier</button>
                  </div>
                  <div className="send-btns">
                    <a className="btn-primary" href={`sms:${clientTel}?body=${encodeURIComponent('Déclaration GAM : ' + lienGenere.url)}`}>Envoyer par SMS</a>
                    <a className="btn-ghost" href={`mailto:${clientEmail}?subject=Déclaration de sinistre&body=${encodeURIComponent(lienGenere.url)}`}>Envoyer par email</a>
                  </div>
                </div>
              )}
            </form>
          )}
        </>
      )}
    </div>
  );
}
