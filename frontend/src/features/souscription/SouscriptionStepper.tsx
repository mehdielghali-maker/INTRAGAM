import { useEffect, useRef, useState } from 'react';
import {
  brouillonsLocaux,
  catalogueSouscription,
  Entite,
  genererReference,
  peutEnvoyer,
  piecesManquantes,
  Piece,
  souscription,
  Souscription,
} from '@souscription';
import { ApercusControle, definition, identifiant, PieceCapturee, PiecesCapture } from '@sinistre-ui';
import { creerAutoEnregistrement } from '@dossier';

type Etape = 1 | 2 | 3;

/**
 * Parcours de souscription auto (AGA, PRÉSENTIEL par défaut) : produit & assuré (OCR seam) → pièces
 * → contrôle. Enregistrable en BROUILLON à tout moment + AUTO-SAVE débouncé (anti-perte). La VALIDATION
 * (enregistrement GAM) n'est possible qu'une fois complet et envoie seule le dossier (un brouillon reste
 * LOCAL). Reprise possible via la prop `brouillon`.
 */
export default function SouscriptionStepper({
  entite,
  agence,
  onTermine,
  brouillon,
}: {
  entite: Entite;
  agence?: { code: string; nom: string };
  onTermine: (message: string) => void;
  brouillon?: Souscription;
}) {
  const [etape, setEtape] = useState<Etape>(1);
  const [prenom, setPrenom] = useState(brouillon?.assure.prenom ?? '');
  const [nom, setNom] = useState(brouillon?.assure.nom ?? entite.nomClient);
  const [numeroCni, setNumeroCni] = useState(brouillon?.assure.numeroCni ?? '');
  const [telephone, setTelephone] = useState(brouillon?.assure.telephone ?? '');
  const [immatriculation, setImmat] = useState(brouillon?.vehicule.immatriculation ?? entite.immatriculation ?? '');
  const [marque, setMarque] = useState(brouillon?.vehicule.marque ?? entite.marque ?? '');
  const [pieces, setPieces] = useState<Piece[]>(brouillon?.pieces ?? []);
  const [message, setMessage] = useState<string | null>(null);
  const [enregistre, setEnregistre] = useState(false);

  // Identité STABLE : reprise d'un brouillon, ou nouvelle générée une seule fois.
  const idLocalRef = useRef(brouillon?.idLocal ?? identifiant('s'));
  const referenceRef = useRef(brouillon?.reference ?? genererReference());

  const complet = peutEnvoyer(pieces);
  const manquantes = piecesManquantes(pieces);

  function construire(statut: Souscription['statut']): Souscription {
    return {
      idLocal: idLocalRef.current,
      reference: referenceRef.current,
      typeProduit: 'AUTO',
      codeBranche: entite.codeBranche,
      libelleBranche: entite.libelleBranche,
      codeSousBranche: entite.codeSousBranche,
      libelleSousBranche: entite.libelleSousBranche,
      numeroPolice: entite.numeroPolice,
      nomClient: entite.nomClient,
      assure: { prenom, nom, numeroCni, telephone },
      vehicule: { immatriculation, marque },
      pieces,
      statut,
      origine: 'AGA',
      agence,
      dateSaisie: brouillon?.dateSaisie ?? new Date().toISOString().slice(0, 10),
    };
  }

  // Auto-enregistrement DÉBOUNCÉ (anti-perte) : sauvegarde le brouillon LOCAL à chaque modification.
  const autoRef = useRef(
    creerAutoEnregistrement<Souscription>(async (s) => {
      await brouillonsLocaux.enregistrer(s);
      setEnregistre(true);
    }, 800),
  );
  useEffect(() => {
    const aDuContenu =
      Boolean(brouillon) ||
      Boolean(prenom || numeroCni.trim() || telephone.trim() || immatriculation.trim() || marque.trim() || pieces.length);
    if (!aDuContenu) return;
    setEnregistre(false);
    autoRef.current.planifier(construire('BROUILLON'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [prenom, nom, numeroCni, telephone, immatriculation, marque, pieces]);
  useEffect(() => {
    const auto = autoRef.current;
    return () => auto.flush();
  }, []);

  function ajouter(p: PieceCapturee) {
    setPieces((prev) => [...prev.filter((x) => x.type !== p.type), p as Piece]);
  }
  function supprimer(type: string) {
    setPieces((prev) => prev.filter((p) => p.type !== type));
  }

  /** Démo du seam OCR : pré-remplit l'assuré depuis la CNI recto capturée. */
  async function prereremplirOcr() {
    const cni = pieces.find((p) => p.type === 'cni_recto');
    if (!cni?.dataUrl) {
      setMessage('Capturez la CNI (recto) pour pré-remplir par OCR.');
      return;
    }
    const champs = await souscription.getOcrData(cni.dataUrl, 'cni_recto');
    if (champs.prenom) setPrenom(champs.prenom);
    if (champs.nom) setNom(champs.nom);
    if (champs.numero) setNumeroCni(champs.numero);
    setMessage('Champs pré-remplis depuis la CNI (OCR — données de démo).');
  }

  async function enregistrerBrouillon() {
    autoRef.current.annuler();
    await brouillonsLocaux.enregistrer(construire('BROUILLON'));
    onTermine(`Brouillon ${referenceRef.current} enregistré — vous pourrez le reprendre depuis le suivi.`);
  }

  async function valider() {
    autoRef.current.annuler();
    const s = construire('A_VALIDER');
    if (!navigator.onLine) {
      await souscription.creerSouscription(s);
      await brouillonsLocaux.supprimer(idLocalRef.current);
      onTermine(`Souscription ${s.reference} enregistrée hors-ligne — finalisée à la reconnexion.`);
      return;
    }
    const enregistree = await souscription.enregistrerSouscription(s, []);
    await brouillonsLocaux.supprimer(idLocalRef.current);
    onTermine(`Souscription ${enregistree.reference} validée et enregistrée pour ${entite.nomClient}.`);
  }

  const boutonBrouillon = (
    <button type="button" className="btn-ghost" onClick={enregistrerBrouillon}>Enregistrer en brouillon</button>
  );

  return (
    <div>
      <div className="sin-stepper">
        {[1, 2, 3].map((n) => (
          <span key={n} className={`sin-pdot ${etape >= (n as Etape) ? 'on' : ''}`} />
        ))}
      </div>
      <p className="hint" style={{ marginTop: -6 }}>
        Saisie par l'AGA (présentiel). {enregistre ? 'Brouillon enregistré automatiquement ✓' : 'Enregistrement automatique anti-perte.'}
      </p>

      {message && <p style={{ color: 'var(--vert)', fontWeight: 600, marginBottom: 10 }}>{message}</p>}

      {etape === 1 && (
        <div>
          <div className="sec-head"><h3>Produit & assuré</h3><span className="ro-tag">Auto · police {entite.numeroPolice}</span></div>
          <div className="grid3">
            <div className="field"><label>Type de produit</label><input className="ro" value="Automobile" readOnly /></div>
            <div className="field"><label>Branche</label><input className="ro" value={entite.libelleBranche} readOnly /></div>
            <div className="field"><label>N° police</label><input className="ro" value={entite.numeroPolice} readOnly /></div>
            <div className="field"><label>Prénom assuré</label><input value={prenom} onChange={(e) => setPrenom(e.target.value)} /></div>
            <div className="field"><label>Nom assuré</label><input value={nom} onChange={(e) => setNom(e.target.value)} /></div>
            <div className="field"><label>N° CNI</label><input value={numeroCni} onChange={(e) => setNumeroCni(e.target.value)} /></div>
            <div className="field"><label>Téléphone</label><input value={telephone} onChange={(e) => setTelephone(e.target.value)} /></div>
            <div className="field"><label>Immatriculation</label><input value={immatriculation} onChange={(e) => setImmat(e.target.value)} /></div>
            <div className="field"><label>Marque / modèle</label><input value={marque} onChange={(e) => setMarque(e.target.value)} /></div>
          </div>
          <div className="form-actions">{boutonBrouillon}<button className="btn-primary" onClick={() => setEtape(2)}>Continuer vers les pièces</button></div>
        </div>
      )}

      {etape === 2 && (
        <div>
          <div className="sec-head"><h3>Pièces &amp; photos</h3><span className="ro-tag">CNI + permis + carte grise + 4 faces obligatoires</span></div>
          <PiecesCapture catalogue={catalogueSouscription} contexte={{}} pieces={pieces} layout="grille" onAjouter={ajouter} onSupprimer={supprimer} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(1)}>Retour</button>
            <button className="btn-ghost" onClick={prereremplirOcr}>Pré-remplir l'assuré (OCR)</button>
            {boutonBrouillon}
            <button className="btn-primary" onClick={() => setEtape(3)}>Contrôler</button>
          </div>
        </div>
      )}

      {etape === 3 && (
        <div>
          <div className="sec-head"><h3>Contrôle &amp; validation</h3></div>
          <ApercusControle catalogue={catalogueSouscription} contexte={{}} pieces={pieces} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(2)}>Retour aux pièces</button>
            {boutonBrouillon}
            <button className="btn-primary" disabled={!complet} onClick={valider}>Valider la souscription</button>
          </div>
          {!complet && (
            <p className="hint" style={{ marginTop: 6 }}>
              Validation impossible — pièces obligatoires manquantes :{' '}
              {manquantes.map((t) => definition(catalogueSouscription, t)?.libelle ?? t).join(', ')}.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
