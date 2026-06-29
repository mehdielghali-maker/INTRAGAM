import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { catalogueDeclaration, CODE_OTP_DEMO, decsin, Declaration, peutEnvoyer, Piece } from '@decsin';
import { ApercusControle, PieceCapturee, PiecesCapture, VerificationPlaque } from '@sinistre-ui';
import { creerAutoEnregistrement } from '@dossier';
import { analyseurReco } from './reco';
import { ajouterPiece, enregistrerDeclaration, getDeclarationLocale, piecesDe, supprimerPiece } from './offline/db';
import { demanderPersistance } from './offline/persist';
import { activerSyncAuto, synchroniser } from './offline/sync';
import { deconnecter, ouvrirSession, sessionCourante } from './session';
import './app.css';

type Etape = 1 | 2 | 3 | 4 | 5; // identification, détails, pièces, récap/contrôle, confirmation

const ENVOI = 'M22 2 11 13M22 2l-7 20-4-9-9-4z';
const CHECK = 'M5 12l5 5L20 6';
const OFFLINE = 'M1 1l22 22M16.7 11.1A5 5 0 0 0 9 6M5 12.5a8 8 0 0 1 2-1.7M12 20h.01';

function codeDepuisUrl(): string {
  return new URLSearchParams(window.location.search).get('code') ?? 'DEC-0000-0000';
}

export default function App() {
  const [code] = useState(codeDepuisUrl);
  const [etape, setEtape] = useState<Etape>(1);
  const [declaration, setDeclaration] = useState<Declaration | null>(null);
  const [pieces, setPieces] = useState<Piece[]>([]);
  const [enLigne, setEnLigne] = useState(navigator.onLine);
  const [persistRefusee, setPersistRefusee] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  // Identification (double facteur)
  const [telephone, setTelephone] = useState('');
  const [otp, setOtp] = useState('');

  const [statutSync, setStatutSync] = useState<'en_attente' | 'synchronise' | 'erreur'>('en_attente');

  // Démarrage : persistance, synchro auto, état réseau, reprise de session.
  useEffect(() => {
    void demanderPersistance().then((ok) => setPersistRefusee(!ok));
    const stop = activerSyncAuto();
    const on = () => setEnLigne(true);
    const off = () => setEnLigne(false);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    void synchroniser();
    // Reprise : session valide → on saute l'identification si une déclaration locale existe.
    const session = sessionCourante();
    if (session) {
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
    const existante = await chercherLocaleParCode(code);
    if (existante) {
      setDeclaration(existante);
      await rechargerPieces(existante.idLocal);
      setEtape(existante.dateSinistre ? 3 : 2);
    }
  }

  async function chercherLocaleParCode(c: string): Promise<Declaration | null> {
    // idLocal dérivé du code pour une reprise déterministe (idempotence).
    return (await getDeclarationLocale(`local-${c}`)) ?? null;
  }

  async function identifier(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    const login = await decsin.loginConducteur(telephone.trim(), otp.trim());
    if (login.code !== 0) {
      setErreur(login.message || 'Code incorrect.');
      return;
    }
    ouvrirSession(login, telephone.trim());
    // Pré-remplissage véhicule depuis la déclaration (code) ou le 1er véhicule du conducteur.
    let base: Declaration | null = null;
    try {
      base = await decsin.getDeclarationParCode(code);
    } catch {
      base = null;
    }
    const vehicule = base ?? login.vehicules[0];
    const decl: Declaration = {
      idLocal: `local-${code}`,
      code,
      origine: 'CLIENT',
      // Le client remplit un lien envoyé par l'AGA ; le dossier devient « à valider » À L'ENVOI.
      statut: 'LIEN_ENVOYE',
      immatriculation: vehicule?.immatriculation ?? '',
      marque: vehicule?.marque ?? '',
      numPolice: vehicule?.numPolice ?? '',
      conducteur: login.nomConducteur,
      dateSinistre: base?.dateSinistre ?? '',
      heureSinistre: base?.heureSinistre ?? '',
      lieuSinistre: base?.lieuSinistre ?? '',
      observations: base?.observations ?? '',
      blesses: base?.blesses ?? false,
      compagnieAdverse: base?.compagnieAdverse,
      vehiculeAdverse: base?.vehiculeAdverse,
      telAssure: telephone.trim(),
      pieces: [],
      client: { nom: login.nomConducteur, telephone: telephone.trim() },
    };
    await enregistrerDeclaration(decl, 'brouillon');
    setDeclaration(decl);
    await rechargerPieces(decl.idLocal);
    setEtape(2);
  }

  const majChamp = useCallback(
    (champ: keyof Declaration, valeur: string | boolean) => {
      setDeclaration((d) => (d ? { ...d, [champ]: valeur } : d));
    },
    [],
  );

  // Auto-enregistrement DÉBOUNCÉ (anti-perte) : sauvegarde le brouillon local à chaque modification
  // des champs (étapes de saisie), en plus des sauvegardes explicites. Les photos sont déjà persistées
  // immédiatement (Dexie). N'écrase pas un dossier déjà envoyé (étape 5).
  const autoRef = useRef(
    creerAutoEnregistrement<Declaration>(async (d) => {
      await enregistrerDeclaration(d, 'brouillon');
    }, 800),
  );
  useEffect(() => {
    if (!declaration || etape < 2 || etape > 4) return;
    autoRef.current.planifier(declaration);
  }, [declaration, etape]);
  useEffect(() => {
    const auto = autoRef.current;
    return () => auto.flush();
  }, []);

  // Pièces : stockées dans le dossier interne (Dexie) ; l'état reflète la base.
  const rechargerPieces = useCallback(async (idLocal: string) => {
    const liste = await piecesDe(idLocal);
    setPieces(liste.map((p) => ({ id: p.id, type: p.type, nom: p.nom, estPdf: p.estPdf, tailleKo: p.tailleKo, dataUrl: p.dataUrl })));
  }, []);

  async function ajouterPieceLocale(piece: PieceCapturee, blob: Blob) {
    if (!declaration) return;
    await ajouterPiece(declaration.idLocal, piece as Piece, blob);
    await rechargerPieces(declaration.idLocal);
  }
  async function retirerPiece(type: string) {
    if (!declaration) return;
    await supprimerPiece(declaration.idLocal, type as Piece['type']);
    await rechargerPieces(declaration.idLocal);
  }

  async function versEtape3(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    if (!declaration) {
      return;
    }
    if (!declaration.dateSinistre || !declaration.heureSinistre || !declaration.lieuSinistre.trim() || !declaration.observations.trim()) {
      setErreur('Date, heure, lieu et circonstances sont obligatoires.');
      return;
    }
    await enregistrerDeclaration(declaration, 'brouillon');
    setEtape(3);
  }

  const tiers = Boolean(declaration?.compagnieAdverse?.trim() || declaration?.vehiculeAdverse?.trim());
  const envoiPossible = declaration ? peutEnvoyer({ pieces, compagnieAdverse: declaration.compagnieAdverse, vehiculeAdverse: declaration.vehiculeAdverse }) : false;

  async function envoyer() {
    if (!declaration) {
      return;
    }
    // À l'envoi, le dossier client passe « à valider » (l'AGA contrôlera puis rattachera PROASSUR).
    await enregistrerDeclaration({ ...declaration, pieces, statut: 'A_VALIDER' }, 'en_attente');
    if (navigator.onLine) {
      await synchroniser();
    }
    await rafraichirStatut();
    setEtape(5);
  }

  async function rafraichirStatut() {
    if (!declaration) {
      return;
    }
    const local = await getDeclarationLocale(declaration.idLocal);
    if (local && local.statutSync !== 'brouillon') {
      setStatutSync(local.statutSync);
    }
  }

  async function reessayer() {
    await synchroniser();
    await rafraichirStatut();
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
            <path d="M5 16l1.5-5h11L19 16M5 16h14v3H5zM7.5 16v-3M16.5 16v-3" />
          </svg>
        </span>
        <div>
          <div className="ht">Déclaration de sinistre</div>
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
      {persistRefusee && (
        <div className="alerte-persist">
          ⚠️ Stockage non garanti par le navigateur : synchronisez dès que possible pour ne rien perdre.
        </div>
      )}

      <div className="progress">
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} className={`pdot ${etape >= (n as Etape) ? 'on' : ''}`} />
        ))}
      </div>

      {etape === 1 && (
        <form className="screen" onSubmit={identifier}>
          <div className="s-title">Vérifions votre identité</div>
          <div className="s-sub">Votre agence GAM vous a envoyé ce lien pour déclarer votre sinistre.</div>
          <div className="code-band">
            <div className="l">Code de votre déclaration</div>
            <div className="v">{code}</div>
          </div>
          {erreur && <div className="bloc-ko">{erreur}</div>}
          <div className="field">
            <label>Votre numéro de téléphone</label>
            <input value={telephone} inputMode="tel" placeholder="0661 …" onChange={(e) => setTelephone(e.target.value)} />
          </div>
          <div className="field">
            <label>Code reçu par SMS</label>
            <input value={otp} inputMode="numeric" placeholder={`Ex. ${CODE_OTP_DEMO} (démo)`} onChange={(e) => setOtp(e.target.value)} />
          </div>
          <button type="submit" className="btn btn-primary" disabled={!telephone || !otp}>Continuer</button>
        </form>
      )}

      {etape === 2 && declaration && (
        <form className="screen" onSubmit={versEtape3}>
          <div className="s-title">Détails du sinistre</div>
          <div className="s-sub">Véhicule {declaration.marque} · {declaration.immatriculation}</div>
          {erreur && <div className="bloc-ko">{erreur}</div>}
          <div className="field"><label>Date du sinistre</label><input type="date" value={declaration.dateSinistre} onChange={(e) => majChamp('dateSinistre', e.target.value)} /></div>
          <div className="field"><label>Heure</label><input type="time" value={declaration.heureSinistre} onChange={(e) => majChamp('heureSinistre', e.target.value)} /></div>
          <div className="field"><label>Lieu du sinistre</label><input value={declaration.lieuSinistre} placeholder="Ex. RN5, Rouiba" onChange={(e) => majChamp('lieuSinistre', e.target.value)} /></div>
          <div className="field"><label>Circonstances</label><textarea value={declaration.observations} placeholder="Décrivez ce qui s'est passé…" onChange={(e) => majChamp('observations', e.target.value)} /></div>
          <div className="field"><label>Compagnie adverse (si tiers)</label><input value={declaration.compagnieAdverse ?? ''} placeholder="Si tiers" onChange={(e) => majChamp('compagnieAdverse', e.target.value)} /></div>
          <div className="field"><label>Véhicule adverse (si tiers)</label><input value={declaration.vehiculeAdverse ?? ''} placeholder="Immat. tiers" onChange={(e) => majChamp('vehiculeAdverse', e.target.value)} /></div>
          <label className="check"><input type="checkbox" checked={declaration.blesses} onChange={(e) => majChamp('blesses', e.target.checked)} /> Y a-t-il des blessés ?</label>
          <div className="save-note">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={CHECK} /></svg>
            Saisie enregistrée automatiquement sur votre téléphone — rien n'est perdu, même sans réseau.
          </div>
          <button type="submit" className="btn btn-primary">Continuer vers les photos</button>
        </form>
      )}

      {etape === 3 && declaration && (
        <div className="screen">
          <div className="s-title">Photos &amp; documents</div>
          <div className="s-sub">Ajoutez les pièces. Les pièces obligatoires (*) sont nécessaires pour envoyer.</div>
          <PiecesCapture catalogue={catalogueDeclaration} contexte={{ tiers }} pieces={pieces} layout="liste" onAjouter={ajouterPieceLocale} onSupprimer={retirerPiece} analyser={analyseurReco} />
          <div className="save-note">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={CHECK} /></svg>
            Vos photos sont enregistrées sur votre téléphone, dans l'application. Rien n'est perdu même sans réseau.
          </div>
          <button className="btn btn-primary" onClick={() => setEtape(4)}>Vérifier ma déclaration</button>
        </div>
      )}

      {etape === 4 && declaration && (
        <div className="screen">
          <div className="s-title">Vérification</div>
          <div className="s-sub">Contrôlez vos pièces avant l'envoi. Touchez une vignette pour l'agrandir.</div>
          <ApercusControle catalogue={catalogueDeclaration} contexte={{ tiers }} pieces={pieces} />
          <VerificationPlaque pieces={pieces} immatriculation={declaration.immatriculation} analyser={analyseurReco} />
          <button className="btn btn-primary" disabled={!envoiPossible} onClick={envoyer}>
            Envoyer ma déclaration
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d={ENVOI} /></svg>
          </button>
          {!envoiPossible && <p className="bloc-ko" style={{ marginTop: 10 }}>Il manque des pièces obligatoires (recto/verso constat, 4 faces, assurance adverse si tiers).</p>}
          <button className="btn btn-ghost" onClick={() => setEtape(3)}>Retour aux photos</button>
        </div>
      )}

      {etape === 5 && declaration && (
        <div className="screen conf-wrap">
          <div className={`conf-ic ${statutSync === 'synchronise' ? '' : 'off'}`}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}><path d={CHECK} /></svg>
          </div>
          <div className="conf-title">{statutSync === 'synchronise' ? 'Déclaration envoyée' : 'Enregistrée sur votre téléphone'}</div>
          <div className="conf-sub">
            {statutSync === 'synchronise'
              ? <>Votre déclaration <b>{declaration.code}</b> a bien été transmise à GAM.</>
              : <>Votre déclaration <b>{declaration.code}</b> et vos photos sont sauvegardées. Elles seront envoyées automatiquement dès que vous aurez du réseau.</>}
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
          {statutSync !== 'synchronise' && (
            <button className="btn btn-ghost" style={{ marginTop: 20 }} onClick={reessayer}>Réessayer l'envoi</button>
          )}
        </div>
      )}
    </div>
  );
}
