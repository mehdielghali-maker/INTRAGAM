import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { catalogueSouscription, CODE_OTP_AGENT, peutEnvoyer, Piece, souscription, Souscription } from '@souscription';
import { ApercusControle, PieceCapturee, PiecesCapture } from '@sinistre-ui';
import { creerAutoEnregistrement } from '@dossier';
import { ajouterPiece, enregistrerSouscriptionLocale, getSouscriptionLocale, piecesDe, supprimerPiece } from './offline/db';
import { demanderPersistance } from './offline/persist';
import { activerSyncAuto, synchroniser } from './offline/sync';
import { deconnecter, ouvrirSession, sessionCourante } from './session';
import './app.css';

type Etape = 1 | 2 | 3 | 4 | 5; // identification, assuré/véhicule, pièces, récap, confirmation

const ENVOI = 'M22 2 11 13M22 2l-7 20-4-9-9-4z';
const CHECK = 'M5 12l5 5L20 6';
const OFFLINE = 'M1 1l22 22M16.7 11.1A5 5 0 0 0 9 6M5 12.5a8 8 0 0 1 2-1.7M12 20h.01';

function referenceDepuisUrl(): string {
  return new URLSearchParams(window.location.search).get('reference') ?? 'SCR-0000-0000';
}

export default function App() {
  const [reference] = useState(referenceDepuisUrl);
  const [etape, setEtape] = useState<Etape>(1);
  const [souscriptionEnCours, setSouscription] = useState<Souscription | null>(null);
  const [pieces, setPieces] = useState<Piece[]>([]);
  const [enLigne, setEnLigne] = useState(navigator.onLine);
  const [persistRefusee, setPersistRefusee] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const [telephone, setTelephone] = useState('');
  const [otp, setOtp] = useState('');
  const [statutSync, setStatutSync] = useState<'en_attente' | 'synchronise' | 'erreur'>('en_attente');

  const idLocal = `local-${reference}`;

  useEffect(() => {
    void demanderPersistance().then((ok) => setPersistRefusee(!ok));
    const stop = activerSyncAuto();
    const on = () => setEnLigne(true);
    const off = () => setEnLigne(false);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    void synchroniser();
    if (sessionCourante()) {
      void reprendre();
    }
    return () => {
      stop();
      window.removeEventListener('online', on);
      window.removeEventListener('offline', off);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function reprendre() {
    const existante = await getSouscriptionLocale(idLocal);
    if (existante) {
      setSouscription(existante);
      await rechargerPieces(existante.idLocal);
      setEtape(2);
    }
  }

  async function identifier(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    const login = await souscription.loginClient(telephone.trim(), otp.trim());
    if (login.code !== 0) {
      setErreur(login.message || 'Code incorrect.');
      return;
    }
    ouvrirSession(login.nomClient ?? 'Client', telephone.trim(), reference);
    // Pré-remplissage depuis la souscription initialisée par l'AGA (récupérée par référence).
    let base: Souscription | null = null;
    try {
      base = await souscription.getSouscriptionParReference(reference);
    } catch {
      base = null;
    }
    const s: Souscription = base
      ? { ...base, idLocal, statut: 'LIEN_ENVOYE', origine: 'CLIENT', assure: { ...base.assure, telephone: telephone.trim() } }
      : {
          idLocal,
          reference,
          typeProduit: 'AUTO',
          codeBranche: '',
          numeroPolice: '',
          nomClient: login.nomClient ?? '',
          assure: { telephone: telephone.trim() },
          vehicule: {},
          pieces: [],
          statut: 'LIEN_ENVOYE',
          origine: 'CLIENT',
        };
    await enregistrerSouscriptionLocale(s, 'brouillon');
    setSouscription(s);
    await rechargerPieces(s.idLocal);
    setEtape(2);
  }

  const rechargerPieces = useCallback(async (id: string) => {
    const liste = await piecesDe(id);
    setPieces(liste.map((p) => ({ id: p.id, type: p.type, nom: p.nom, estPdf: p.estPdf, tailleKo: p.tailleKo, dataUrl: p.dataUrl })));
  }, []);

  function majAssure(champ: 'prenom' | 'nom' | 'numeroCni', valeur: string) {
    setSouscription((s) => (s ? { ...s, assure: { ...s.assure, [champ]: valeur } } : s));
  }
  function majVehicule(champ: 'immatriculation' | 'marque', valeur: string) {
    setSouscription((s) => (s ? { ...s, vehicule: { ...s.vehicule, [champ]: valeur } } : s));
  }

  // Auto-enregistrement DÉBOUNCÉ (anti-perte) sur les champs de saisie (étapes 2-4).
  const autoRef = useRef(
    creerAutoEnregistrement<Souscription>(async (s) => {
      await enregistrerSouscriptionLocale(s, 'brouillon');
    }, 800),
  );
  useEffect(() => {
    if (!souscriptionEnCours || etape < 2 || etape > 4) return;
    autoRef.current.planifier(souscriptionEnCours);
  }, [souscriptionEnCours, etape]);
  useEffect(() => {
    const auto = autoRef.current;
    return () => auto.flush();
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

  async function envoyer() {
    if (!souscriptionEnCours) return;
    // À l'envoi, le dossier passe « à valider » : l'AGA contrôle puis valide (enregistrement GAM).
    await enregistrerSouscriptionLocale({ ...souscriptionEnCours, pieces, statut: 'A_VALIDER' }, 'en_attente');
    if (navigator.onLine) {
      await synchroniser();
    }
    const local = await getSouscriptionLocale(souscriptionEnCours.idLocal);
    if (local && local.statutSync !== 'brouillon') {
      setStatutSync(local.statutSync);
    }
    setEtape(5);
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
    <div className={`app ${enLigne ? '' : 'offline'}`}>
      <header className="app-head">
        <span className="lg">
          <svg viewBox="0 0 24 24" fill="none" stroke="#0A3D12" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
            <rect x="5" y="3" width="14" height="18" rx="2" /><path d="M5 8h14M9 13l2 2 4-4" />
          </svg>
        </span>
        <div>
          <div className="ht">Souscription auto</div>
          <div className="hs">GAM Assurances</div>
        </div>
        {sessionCourante() && <button className="deco" onClick={seDeconnecter}>Déconnexion</button>}
      </header>

      {!enLigne && (
        <div className="offline-banner">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={OFFLINE} /></svg>
          Hors-ligne — vos données sont enregistrées sur votre téléphone
        </div>
      )}
      {persistRefusee && <div className="alerte-persist">⚠️ Stockage non garanti : synchronisez dès que possible.</div>}

      <div className="progress">
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} className={`pdot ${etape >= (n as Etape) ? 'on' : ''}`} />
        ))}
      </div>

      {etape === 1 && (
        <form className="screen" onSubmit={identifier}>
          <div className="s-title">Vérifions votre identité</div>
          <div className="s-sub">Votre agence GAM vous a envoyé ce lien pour finaliser votre souscription.</div>
          <div className="code-band">
            <div className="l">Référence de votre souscription</div>
            <div className="v">{reference}</div>
          </div>
          {erreur && <div className="bloc-ko">{erreur}</div>}
          <div className="field"><label>Votre numéro de téléphone</label><input value={telephone} inputMode="tel" placeholder="0661 …" onChange={(e) => setTelephone(e.target.value)} /></div>
          <div className="field"><label>Code reçu par SMS</label><input value={otp} inputMode="numeric" placeholder={`Ex. ${CODE_OTP_AGENT} (démo)`} onChange={(e) => setOtp(e.target.value)} /></div>
          <button type="submit" className="btn btn-primary" disabled={!telephone || !otp}>Continuer</button>
        </form>
      )}

      {etape === 2 && souscriptionEnCours && (
        <div className="screen">
          <div className="s-title">Assuré &amp; véhicule</div>
          <div className="s-sub">Police {souscriptionEnCours.numeroPolice || '—'}</div>
          <div className="field"><label>Prénom</label><input value={souscriptionEnCours.assure.prenom ?? ''} onChange={(e) => majAssure('prenom', e.target.value)} /></div>
          <div className="field"><label>Nom</label><input value={souscriptionEnCours.assure.nom ?? ''} onChange={(e) => majAssure('nom', e.target.value)} /></div>
          <div className="field"><label>N° CNI</label><input value={souscriptionEnCours.assure.numeroCni ?? ''} onChange={(e) => majAssure('numeroCni', e.target.value)} /></div>
          <div className="field"><label>Immatriculation</label><input value={souscriptionEnCours.vehicule.immatriculation ?? ''} onChange={(e) => majVehicule('immatriculation', e.target.value)} /></div>
          <div className="field"><label>Marque / modèle</label><input value={souscriptionEnCours.vehicule.marque ?? ''} onChange={(e) => majVehicule('marque', e.target.value)} /></div>
          <div className="save-note">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={CHECK} /></svg>
            Saisie enregistrée automatiquement sur votre téléphone — rien n'est perdu, même sans réseau.
          </div>
          <button className="btn btn-primary" onClick={() => setEtape(3)}>Continuer vers les pièces</button>
        </div>
      )}

      {etape === 3 && souscriptionEnCours && (
        <div className="screen">
          <div className="s-title">Pièces &amp; photos</div>
          <div className="s-sub">CNI + permis + carte grise + 4 faces du véhicule.</div>
          <PiecesCapture catalogue={catalogueSouscription} contexte={{}} pieces={pieces} layout="liste" onAjouter={ajouter} onSupprimer={retirer} />
          <button className="btn btn-primary" onClick={() => setEtape(4)}>Vérifier ma souscription</button>
        </div>
      )}

      {etape === 4 && souscriptionEnCours && (
        <div className="screen">
          <div className="s-title">Vérification</div>
          <div className="s-sub">Contrôlez vos pièces avant l'envoi. Touchez une vignette pour l'agrandir.</div>
          <ApercusControle catalogue={catalogueSouscription} contexte={{}} pieces={pieces} />
          <button className="btn btn-primary" disabled={!peutEnvoyer(pieces)} onClick={envoyer}>
            Envoyer ma souscription
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={ENVOI} /></svg>
          </button>
          {!peutEnvoyer(pieces) && <p className="bloc-ko" style={{ marginTop: 10 }}>Pièces obligatoires manquantes (CNI, permis, carte grise, 4 faces).</p>}
          <button className="btn btn-ghost" onClick={() => setEtape(3)}>Retour aux pièces</button>
        </div>
      )}

      {etape === 5 && souscriptionEnCours && (
        <div className="screen conf-wrap">
          <div className={`conf-ic ${statutSync === 'synchronise' ? '' : 'off'}`}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}><path d={CHECK} /></svg>
          </div>
          <div className="conf-title">{statutSync === 'synchronise' ? 'Souscription transmise' : 'Enregistrée sur votre téléphone'}</div>
          <div className="conf-sub">
            {statutSync === 'synchronise'
              ? <>Votre souscription <b>{souscriptionEnCours.reference}</b> a été transmise à votre agence pour validation.</>
              : <>Votre souscription <b>{souscriptionEnCours.reference}</b> et vos photos sont sauvegardées ; envoi automatique au retour du réseau.</>}
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
          {statutSync !== 'synchronise' && <button className="btn btn-ghost" style={{ marginTop: 20 }} onClick={reessayer}>Réessayer l'envoi</button>}
        </div>
      )}
    </div>
  );
}
