import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  catalogueSouscription,
  CODE_OTP_AGENT,
  Entite,
  genererReference,
  peutEnvoyer,
  Piece,
  souscription,
  Souscription,
} from '@souscription';
import { ApercusControle, identifiant, PieceCapturee, PiecesCapture } from '@sinistre-ui';
import { ajouterPiece, enregistrerSouscriptionLocale, getSouscriptionLocale, piecesDe, supprimerPiece } from './offline/db';
import { demanderPersistance } from './offline/persist';
import { activerSyncAuto, synchroniser } from './offline/sync';
import { deconnecter, ouvrirSession, sessionCourante } from './session';
import './app.css';

type Etape = 1 | 2 | 3 | 4 | 5 | 6; // login, recherche, détails, pièces, récap, confirmation

const ENVOI = 'M22 2 11 13M22 2l-7 20-4-9-9-4z';
const CHECK = 'M5 12l5 5L20 6';
const OFFLINE = 'M1 1l22 22M16.7 11.1A5 5 0 0 0 9 6M5 12.5a8 8 0 0 1 2-1.7M12 20h.01';

export default function App() {
  const [etape, setEtape] = useState<Etape>(1);
  const [enLigne, setEnLigne] = useState(navigator.onLine);
  const [persistRefusee, setPersistRefusee] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  // Login agent
  const [login, setLogin] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [otp, setOtp] = useState('');

  // Recherche
  const [numeroPolice, setNumeroPolice] = useState('');
  const [nomClient, setNomClient] = useState('');
  const [resultats, setResultats] = useState<Entite[]>([]);

  // Souscription en cours
  const [souscriptionEnCours, setSouscription] = useState<Souscription | null>(null);
  const [pieces, setPieces] = useState<Piece[]>([]);
  const [statutSync, setStatutSync] = useState<'en_attente' | 'synchronise' | 'erreur'>('en_attente');

  useEffect(() => {
    void demanderPersistance().then((ok) => setPersistRefusee(!ok));
    const stop = activerSyncAuto();
    const on = () => setEnLigne(true);
    const off = () => setEnLigne(false);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    void synchroniser();
    if (sessionCourante()) {
      setEtape(2);
    }
    return () => {
      stop();
      window.removeEventListener('online', on);
      window.removeEventListener('offline', off);
    };
  }, []);

  async function identifier(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    const s = await souscription.loginAgent(login.trim(), motDePasse, otp.trim() || undefined);
    if (s.code !== 0) {
      setErreur(s.message || 'Identifiants incorrects.');
      return;
    }
    ouvrirSession(s);
    setEtape(2);
  }

  async function rechercher(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    const r = await souscription.rechercheEntite({ numeroPolice: numeroPolice.trim(), nomClient: nomClient.trim() });
    setResultats(r);
    if (r.length === 0) {
      setErreur('Aucune police trouvée.');
    }
  }

  async function choisir(entite: Entite) {
    const s: Souscription = {
      idLocal: identifiant('s'),
      reference: genererReference(),
      typeProduit: 'AUTO',
      codeBranche: entite.codeBranche,
      libelleBranche: entite.libelleBranche,
      codeSousBranche: entite.codeSousBranche,
      libelleSousBranche: entite.libelleSousBranche,
      numeroPolice: entite.numeroPolice,
      nomClient: entite.nomClient,
      assure: { nom: entite.nomClient },
      vehicule: { immatriculation: entite.immatriculation, marque: entite.marque },
      pieces: [],
      statut: 'BROUILLON',
    };
    await enregistrerSouscriptionLocale(s, 'brouillon');
    setSouscription(s);
    setPieces([]);
    setEtape(3);
  }

  const rechargerPieces = useCallback(async (idLocal: string) => {
    const liste = await piecesDe(idLocal);
    setPieces(liste.map((p) => ({ id: p.id, type: p.type, nom: p.nom, estPdf: p.estPdf, tailleKo: p.tailleKo, dataUrl: p.dataUrl })));
  }, []);

  async function ajouter(piece: PieceCapturee, blob: Blob) {
    if (!souscriptionEnCours) return;
    await ajouterPiece(souscriptionEnCours.idLocal, piece as Piece, blob);
    await rechargerPieces(souscriptionEnCours.idLocal);
  }
  async function retirer(type: string) {
    if (!souscriptionEnCours) return;
    await supprimerPiece(souscriptionEnCours.idLocal, type as Piece['type']);
    await rechargerPieces(souscriptionEnCours.idLocal);
  }

  function majAssure(champ: 'prenom' | 'nom' | 'numeroCni' | 'telephone', valeur: string) {
    setSouscription((s) => (s ? { ...s, assure: { ...s.assure, [champ]: valeur } } : s));
  }

  async function prereremplirOcr() {
    const cni = pieces.find((p) => p.type === 'cni_recto');
    if (!cni?.dataUrl) {
      setErreur('Capturez la CNI (recto) pour pré-remplir par OCR.');
      return;
    }
    const champs = await souscription.getOcrData(cni.dataUrl, 'cni_recto');
    setSouscription((s) => (s ? { ...s, assure: { ...s.assure, prenom: champs.prenom, nom: champs.nom, numeroCni: champs.numero } } : s));
  }

  async function envoyer() {
    if (!souscriptionEnCours) return;
    await enregistrerSouscriptionLocale({ ...souscriptionEnCours, pieces }, 'en_attente');
    if (navigator.onLine) {
      await synchroniser();
    }
    const local = await getSouscriptionLocale(souscriptionEnCours.idLocal);
    if (local && local.statutSync !== 'brouillon') {
      setStatutSync(local.statutSync);
    }
    setEtape(6);
  }

  async function reessayer() {
    await synchroniser();
    if (souscriptionEnCours) {
      const local = await getSouscriptionLocale(souscriptionEnCours.idLocal);
      if (local && local.statutSync !== 'brouillon') {
        setStatutSync(local.statutSync);
      }
    }
  }

  async function seDeconnecter() {
    await deconnecter();
    window.location.reload();
  }

  return (
    <div className="app">
      <header className="app-head">
        <span className="lg">
          <svg viewBox="0 0 24 24" fill="none" stroke="#0A3D12" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
            <rect x="5" y="3" width="14" height="18" rx="2" /><path d="M5 8h14M9 13l2 2 4-4" />
          </svg>
        </span>
        <div>
          <div className="ht">Souscription auto</div>
          <div className="hs">GAM Assurances — espace agent</div>
        </div>
        {sessionCourante() && <button className="deco" onClick={seDeconnecter}>Déconnexion</button>}
      </header>

      {!enLigne && (
        <div className="offline-banner">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={OFFLINE} /></svg>
          Hors-ligne — vos données sont enregistrées sur l'appareil
        </div>
      )}
      {persistRefusee && <div className="alerte-persist">⚠️ Stockage non garanti : synchronisez dès que possible.</div>}

      <div className="progress">
        {[1, 2, 3, 4, 5, 6].map((n) => (
          <span key={n} className={`pdot ${etape >= (n as Etape) ? 'on' : ''}`} />
        ))}
      </div>

      {etape === 1 && (
        <form className="screen" onSubmit={identifier}>
          <div className="s-title">Connexion agent</div>
          <div className="s-sub">Identifiez-vous pour souscrire un contrat.</div>
          {erreur && <div className="bloc-ko">{erreur}</div>}
          <div className="field"><label>Identifiant</label><input value={login} onChange={(e) => setLogin(e.target.value)} /></div>
          <div className="field"><label>Mot de passe</label><input type="password" value={motDePasse} onChange={(e) => setMotDePasse(e.target.value)} /></div>
          <div className="field"><label>Code OTP</label><input inputMode="numeric" placeholder={`Ex. ${CODE_OTP_AGENT} (démo)`} value={otp} onChange={(e) => setOtp(e.target.value)} /></div>
          <button type="submit" className="btn btn-primary" disabled={!login || !motDePasse}>Se connecter</button>
        </form>
      )}

      {etape === 2 && (
        <form className="screen" onSubmit={rechercher}>
          <div className="s-title">Rechercher la police</div>
          <div className="s-sub">Saisissez un numéro de police ou un nom de client.</div>
          {erreur && <div className="bloc-ko">{erreur}</div>}
          <div className="field"><label>N° de police</label><input value={numeroPolice} placeholder="AUTO-2026-…" onChange={(e) => setNumeroPolice(e.target.value)} /></div>
          <div className="field"><label>Nom du client</label><input value={nomClient} onChange={(e) => setNomClient(e.target.value)} /></div>
          <button type="submit" className="btn btn-primary">Rechercher</button>
          {resultats.length > 0 && (
            <div className="res-list" style={{ marginTop: 16 }}>
              {resultats.map((e) => (
                <div key={e.numeroPolice} className="res" onClick={() => choisir(e)}>
                  <div className="r1">{e.numeroPolice}</div>
                  <div className="r2">{e.nomClient} · {e.marque ?? ''} {e.immatriculation ?? ''}</div>
                </div>
              ))}
            </div>
          )}
        </form>
      )}

      {etape === 3 && souscriptionEnCours && (
        <div className="screen">
          <div className="s-title">Assuré & véhicule</div>
          <div className="s-sub">Police {souscriptionEnCours.numeroPolice} · {souscriptionEnCours.vehicule.marque ?? ''}</div>
          <div className="field"><label>Prénom</label><input value={souscriptionEnCours.assure.prenom ?? ''} onChange={(e) => majAssure('prenom', e.target.value)} /></div>
          <div className="field"><label>Nom</label><input value={souscriptionEnCours.assure.nom ?? ''} onChange={(e) => majAssure('nom', e.target.value)} /></div>
          <div className="field"><label>N° CNI</label><input value={souscriptionEnCours.assure.numeroCni ?? ''} onChange={(e) => majAssure('numeroCni', e.target.value)} /></div>
          <div className="field"><label>Téléphone</label><input value={souscriptionEnCours.assure.telephone ?? ''} onChange={(e) => majAssure('telephone', e.target.value)} /></div>
          <button className="btn btn-primary" onClick={() => setEtape(4)}>Continuer vers les pièces</button>
        </div>
      )}

      {etape === 4 && souscriptionEnCours && (
        <div className="screen">
          <div className="s-title">Pièces &amp; photos</div>
          <div className="s-sub">CNI + permis + carte grise + 4 faces véhicule obligatoires.</div>
          <PiecesCapture catalogue={catalogueSouscription} contexte={{}} pieces={pieces} layout="liste" onAjouter={ajouter} onSupprimer={retirer} />
          <button className="btn btn-ghost" onClick={prereremplirOcr}>Pré-remplir l'assuré (OCR)</button>
          <button className="btn btn-primary" onClick={() => setEtape(5)}>Vérifier</button>
        </div>
      )}

      {etape === 5 && souscriptionEnCours && (
        <div className="screen">
          <div className="s-title">Vérification</div>
          <div className="s-sub">Contrôlez les pièces avant l'enregistrement.</div>
          <ApercusControle catalogue={catalogueSouscription} contexte={{}} pieces={pieces} />
          <button className="btn btn-primary" disabled={!peutEnvoyer(pieces)} onClick={envoyer}>
            Enregistrer la souscription
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={ENVOI} /></svg>
          </button>
          {!peutEnvoyer(pieces) && <p className="bloc-ko" style={{ marginTop: 10 }}>Pièces obligatoires manquantes.</p>}
          <button className="btn btn-ghost" onClick={() => setEtape(4)}>Retour aux pièces</button>
        </div>
      )}

      {etape === 6 && souscriptionEnCours && (
        <div className="screen conf-wrap">
          <div className={`conf-ic ${statutSync === 'synchronise' ? '' : 'off'}`}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}><path d={CHECK} /></svg>
          </div>
          <div className="conf-title">{statutSync === 'synchronise' ? 'Souscription transmise' : 'Enregistrée sur l\'appareil'}</div>
          <div className="conf-sub">
            {statutSync === 'synchronise'
              ? <>La souscription <b>{souscriptionEnCours.reference}</b> a été transmise à GAM.</>
              : <>La souscription <b>{souscriptionEnCours.reference}</b> et ses pièces sont sauvegardées ; envoi automatique au retour du réseau.</>}
          </div>
          <div style={{ textAlign: 'center' }}>
            {statutSync === 'synchronise' ? (
              <span className="sync-pill sync-ok"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}><path d={CHECK} /></svg> Synchronisée</span>
            ) : statutSync === 'erreur' ? (
              <span className="sync-pill sync-err">Échec d'envoi — sera réessayé</span>
            ) : (
              <span className="sync-pill sync-wait">En attente d'envoi</span>
            )}
          </div>
          {statutSync !== 'synchronise' && <button className="btn btn-ghost" style={{ marginTop: 20 }} onClick={reessayer}>Réessayer</button>}
          <button className="btn btn-primary" style={{ marginTop: 10 }} onClick={() => { setSouscription(null); setPieces([]); setResultats([]); setNumeroPolice(''); setNomClient(''); setEtape(2); }}>
            Nouvelle souscription
          </button>
        </div>
      )}
    </div>
  );
}
