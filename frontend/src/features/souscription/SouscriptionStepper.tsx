import { useState } from 'react';
import {
  catalogueSouscription,
  Entite,
  genererReference,
  peutEnvoyer,
  Piece,
  souscription,
  Souscription,
} from '@souscription';
import { ApercusControle, identifiant, PieceCapturee, PiecesCapture } from '@sinistre-ui';

type Etape = 1 | 2 | 3;

/**
 * Parcours guidé de souscription auto (AGA, après sélection d'une police) : produit & assuré
 * (pré-remplissage OCR — seam) → capture des pièces (socle partagé, catalogue souscription) →
 * contrôle de complétude → enregistrement.
 */
export default function SouscriptionStepper({
  entite,
  agence,
  onTermine,
}: {
  entite: Entite;
  agence?: { code: string; nom: string };
  onTermine: (message: string) => void;
}) {
  const [etape, setEtape] = useState<Etape>(1);
  const [prenom, setPrenom] = useState('');
  const [nom, setNom] = useState(entite.nomClient);
  const [numeroCni, setNumeroCni] = useState('');
  const [telephone, setTelephone] = useState('');
  const [immatriculation, setImmat] = useState(entite.immatriculation ?? '');
  const [marque, setMarque] = useState(entite.marque ?? '');
  const [pieces, setPieces] = useState<Piece[]>([]);
  const [message, setMessage] = useState<string | null>(null);

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

  function construire(): Souscription {
    return {
      idLocal: identifiant('s'),
      reference: genererReference(),
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
      statut: 'BROUILLON',
      agence,
    };
  }

  async function enregistrer() {
    const s = await souscription.creerSouscription(construire());
    if (!navigator.onLine) {
      onTermine(`Souscription ${s.reference} enregistrée hors-ligne — transmise à la reconnexion.`);
      return;
    }
    const enregistree = await souscription.enregistrerSouscription(s, []);
    onTermine(`Souscription ${enregistree.reference} enregistrée pour ${entite.nomClient}.`);
  }

  return (
    <div>
      <div className="sin-stepper">
        {[1, 2, 3].map((n) => (
          <span key={n} className={`sin-pdot ${etape >= (n as Etape) ? 'on' : ''}`} />
        ))}
      </div>

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
          <div className="form-actions"><button className="btn-primary" onClick={() => setEtape(2)}>Continuer vers les pièces</button></div>
        </div>
      )}

      {etape === 2 && (
        <div>
          <div className="sec-head"><h3>Pièces &amp; photos</h3><span className="ro-tag">CNI + permis + carte grise + 4 faces obligatoires</span></div>
          <PiecesCapture catalogue={catalogueSouscription} contexte={{}} pieces={pieces} layout="grille" onAjouter={ajouter} onSupprimer={supprimer} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(1)}>Retour</button>
            <button className="btn-ghost" onClick={prereremplirOcr}>Pré-remplir l'assuré (OCR)</button>
            <button className="btn-primary" onClick={() => setEtape(3)}>Contrôler</button>
          </div>
        </div>
      )}

      {etape === 3 && (
        <div>
          <div className="sec-head"><h3>Contrôle &amp; enregistrement</h3></div>
          <ApercusControle catalogue={catalogueSouscription} contexte={{}} pieces={pieces} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(2)}>Retour aux pièces</button>
            <button className="btn-primary" disabled={!peutEnvoyer(pieces)} onClick={enregistrer}>Enregistrer la souscription</button>
          </div>
        </div>
      )}
    </div>
  );
}
