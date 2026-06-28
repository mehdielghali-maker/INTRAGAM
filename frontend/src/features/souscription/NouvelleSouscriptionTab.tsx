import { FormEvent, useState } from 'react';
import { Entite, genererReference, souscription, Souscription } from '@souscription';
import { identifiant } from '@sinistre-ui';
import { useAgence } from '../../app/AgencyContext';
import SouscriptionStepper from './SouscriptionStepper';

type Mode = 'saisie' | 'lien';
const URL_CLIENT = (import.meta.env.VITE_SOUSCRIPTION_CLIENT_URL as string) || 'http://localhost:5176';

/**
 * Onglet « Nouvelle souscription » : PRÉSENTIEL par défaut — l'AGA recherche la police puis saisit
 * (brouillon possible). Sait aussi REPRENDRE un brouillon existant (prop `brouillonInitial`).
 */
export default function NouvelleSouscriptionTab({
  brouillonInitial,
  onFini,
}: {
  brouillonInitial?: Souscription | null;
  onFini?: () => void;
} = {}) {
  const { contexte } = useAgence();
  const consolide = contexte?.consolideActif ?? false;
  const agenceActive = contexte?.agenceActive ?? undefined;

  const [numeroPolice, setNumeroPolice] = useState('');
  const [nomClient, setNomClient] = useState('');
  const [resultats, setResultats] = useState<Entite[] | null>(null);
  const [choisie, setChoisie] = useState<Entite | null>(null);
  const [mode, setMode] = useState<Mode>('saisie');
  const [clientNom, setClientNom] = useState('');
  const [clientTel, setClientTel] = useState('');
  const [clientEmail, setClientEmail] = useState('');
  const [lienGenere, setLienGenere] = useState<{ reference: string; url: string } | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function genererLien() {
    setErreur(null);
    if (consolide) {
      setErreur('Action impossible en vue consolidée : sélectionnez une agence.');
      return;
    }
    if (!clientNom.trim() || !clientTel.trim() || !choisie) {
      setErreur('Nom et téléphone du client sont obligatoires.');
      return;
    }
    const reference = genererReference();
    const s: Souscription = {
      idLocal: identifiant('s'), reference, typeProduit: 'AUTO',
      codeBranche: choisie.codeBranche, libelleBranche: choisie.libelleBranche,
      codeSousBranche: choisie.codeSousBranche, libelleSousBranche: choisie.libelleSousBranche,
      numeroPolice: choisie.numeroPolice, nomClient: choisie.nomClient,
      assure: { nom: choisie.nomClient }, vehicule: { immatriculation: choisie.immatriculation, marque: choisie.marque },
      pieces: [], statut: 'LIEN_ENVOYE', origine: 'CLIENT',
      agence: agenceActive ? { code: agenceActive.code, nom: agenceActive.nom } : undefined,
      client: { nom: clientNom.trim(), telephone: clientTel.trim(), email: clientEmail.trim() || undefined },
    };
    await souscription.creerSouscription(s);
    setLienGenere({ reference, url: `${URL_CLIENT}/?reference=${encodeURIComponent(reference)}` });
  }

  async function rechercher(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setMessage(null);
    setChoisie(null);
    const r = await souscription.rechercheEntite({ numeroPolice: numeroPolice.trim(), nomClient: nomClient.trim() });
    setResultats(r);
    if (r.length === 0) {
      setErreur('Aucune police trouvée pour ces critères.');
    }
  }

  function terminer(msg: string) {
    setMessage(msg);
    setChoisie(null);
    setResultats(null);
    setNumeroPolice('');
    setNomClient('');
    onFini?.();
  }

  // --- REPRISE d'un brouillon : ouvre directement le stepper pré-rempli ------------------------
  if (brouillonInitial) {
    const entiteBrouillon: Entite = {
      numeroPolice: brouillonInitial.numeroPolice,
      nomClient: brouillonInitial.nomClient,
      codeBranche: brouillonInitial.codeBranche,
      libelleBranche: brouillonInitial.libelleBranche ?? 'Automobile',
      codeSousBranche: brouillonInitial.codeSousBranche,
      libelleSousBranche: brouillonInitial.libelleSousBranche,
      marque: brouillonInitial.vehicule.marque,
      immatriculation: brouillonInitial.vehicule.immatriculation,
    };
    return (
      <div className="form-card">
        <div className="sec-head">
          <h3>Reprise du brouillon {brouillonInitial.reference}</h3>
          <button type="button" className="btn-ghost" onClick={() => onFini?.()}>Annuler la reprise</button>
        </div>
        <SouscriptionStepper entite={entiteBrouillon} agence={brouillonInitial.agence} brouillon={brouillonInitial} onTermine={terminer} />
      </div>
    );
  }

  return (
    <div className="form-card">
      {erreur && <p className="bloc-msg" style={{ marginBottom: 12 }}>{erreur}</p>}
      {message && <p style={{ color: 'var(--vert)', marginBottom: 12, fontWeight: 600 }}>{message}</p>}

      <form className="loader" onSubmit={rechercher}>
        <div className="field">
          <label>N° de police</label>
          <input value={numeroPolice} placeholder="Ex. AUTO-2026-00123" onChange={(e) => setNumeroPolice(e.target.value)} />
        </div>
        <div className="field">
          <label>Nom du client</label>
          <input value={nomClient} placeholder="Ex. Meziane" onChange={(e) => setNomClient(e.target.value)} />
        </div>
        <button type="submit" className="btn-primary">Rechercher la police</button>
        <span className="hint-strong">Branche & véhicule se chargent depuis GAM</span>
      </form>

      {!choisie && resultats && resultats.length > 0 && (
        <table className="list" style={{ marginTop: 16 }}>
          <thead>
            <tr><th>N° police</th><th>Client</th><th>Branche</th><th>Véhicule</th><th>Action</th></tr>
          </thead>
          <tbody>
            {resultats.map((e) => (
              <tr key={e.numeroPolice}>
                <td><b>{e.numeroPolice}</b></td>
                <td>{e.nomClient}</td>
                <td>{e.libelleBranche}</td>
                <td>{e.marque ?? '—'} {e.immatriculation ? `· ${e.immatriculation}` : ''}</td>
                <td><button className="act-btn act-valider" onClick={() => setChoisie(e)}>Souscrire</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {choisie && (
        <>
          <div className="sec-head"><h3>Mode de souscription</h3></div>
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
              <SouscriptionStepper
                entite={choisie}
                agence={agenceActive ? { code: agenceActive.code, nom: agenceActive.nom } : undefined}
                onTermine={terminer}
              />
            )
          ) : (
            <div>
              <div className="sec-head"><h3>Identification du client</h3></div>
              <div className="grid3">
                <div className="field"><label>Nom <span className="req">*</span></label><input value={clientNom} onChange={(e) => setClientNom(e.target.value)} /></div>
                <div className="field"><label>Téléphone <span className="req">*</span></label><input value={clientTel} onChange={(e) => setClientTel(e.target.value)} /></div>
                <div className="field"><label>Email <span className="opt">— facultatif</span></label><input value={clientEmail} onChange={(e) => setClientEmail(e.target.value)} /></div>
              </div>
              {!lienGenere ? (
                <div className="form-actions"><button type="button" className="btn-primary" onClick={genererLien}>Générer le lien</button></div>
              ) : (
                <div className="link-box" style={{ marginTop: 16 }}>
                  <div className="code-chip">Réf. : {lienGenere.reference}</div>
                  <div className="link-row">
                    <input className="link-url" value={lienGenere.url} readOnly />
                    <button type="button" className="btn-ghost" onClick={() => navigator.clipboard?.writeText(lienGenere.url)}>Copier</button>
                  </div>
                  <div className="send-btns">
                    <a className="btn-primary" href={`sms:${clientTel}?body=${encodeURIComponent('Souscription GAM : ' + lienGenere.url)}`}>Envoyer par SMS</a>
                    <a className="btn-ghost" href={`mailto:${clientEmail}?subject=Souscription auto&body=${encodeURIComponent(lienGenere.url)}`}>Envoyer par email</a>
                  </div>
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
