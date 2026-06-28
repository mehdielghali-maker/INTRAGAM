import { FormEvent, useState } from 'react';
import {
  catalogueDeclaration,
  decsin,
  Declaration,
  genererCode,
  identifiant,
  peutEnvoyer,
  Piece,
  Vehicule,
} from '@decsin';
import { ApercusControle, PieceCapturee, PiecesCapture } from '@sinistre-ui';

type Etape = 1 | 2 | 3;

/**
 * Parcours guidé de capture AGA (poste + mobile) : détails → pièces (socle partagé) →
 * contrôle/validation. L'AGA étant authentifié (SSO + agence), pas d'identification par code.
 * En ligne, « Valider → PROASSUR » rattache et renvoie le N° ; hors-ligne, on enregistre
 * « À valider » (validation différée à la reconnexion).
 */
export default function DeclarationCaptureStepper({
  vehicule,
  onTermine,
}: {
  vehicule: Vehicule;
  onTermine: (message: string) => void;
}) {
  const [etape, setEtape] = useState<Etape>(1);
  const [dateSinistre, setDate] = useState('');
  const [heureSinistre, setHeure] = useState('');
  const [lieuSinistre, setLieu] = useState('');
  const [observations, setObs] = useState('');
  const [blesses, setBlesses] = useState(false);
  const [telAssure, setTel] = useState('');
  const [compagnieAdverse, setCompagnie] = useState('');
  const [vehiculeAdverse, setVehAdverse] = useState('');
  const [pieces, setPieces] = useState<Piece[]>([]);
  const [erreur, setErreur] = useState<string | null>(null);

  const tiers = Boolean(compagnieAdverse.trim() || vehiculeAdverse.trim());

  function ajouter(p: PieceCapturee) {
    setPieces((prev) => [...prev.filter((x) => x.type !== p.type), p as Piece]);
  }
  function supprimer(type: string) {
    setPieces((prev) => prev.filter((p) => p.type !== type));
  }

  function versPieces(e: FormEvent) {
    e.preventDefault();
    if (!dateSinistre || !heureSinistre || !lieuSinistre.trim() || !observations.trim()) {
      setErreur('Date, heure, lieu et circonstances sont obligatoires.');
      return;
    }
    setErreur(null);
    setEtape(2);
  }

  function declarationCourante(statut: Declaration['statut']): Declaration {
    return {
      idLocal: identifiant('d'),
      code: genererCode(),
      origine: 'AGA',
      statut,
      immatriculation: vehicule.immatriculation,
      marque: vehicule.marque,
      numPolice: vehicule.numPolice,
      conducteur: vehicule.conducteur,
      dateSinistre,
      heureSinistre,
      lieuSinistre,
      observations,
      blesses,
      telAssure,
      compagnieAdverse: compagnieAdverse || undefined,
      vehiculeAdverse: vehiculeAdverse || undefined,
      pieces,
    };
  }

  async function valider() {
    if (!navigator.onLine) {
      // Capture OK hors-ligne ; le rattachement PROASSUR (validation) est différé à la reconnexion.
      const enAttente = await decsin.creerDeclaration({ ...declarationCourante('A_VALIDER'), aRattacher: true });
      onTermine(`Déclaration ${enAttente.code} enregistrée hors-ligne — validée automatiquement à la reconnexion.`);
      return;
    }
    const decl = await decsin.creerDeclaration(declarationCourante('A_VALIDER'));
    const { numSinistre, idDossierSinistre } = await decsin.rattacherDeclaration(decl);
    await decsin.creerDeclaration({ ...decl, statut: 'VALIDEE', numSinistre, idDossierSinistre });
    onTermine(`Déclaration ${decl.code} validée — N° sinistre ${numSinistre}.`);
  }

  return (
    <div>
      <div className="sin-stepper">
        {[1, 2, 3].map((n) => (
          <span key={n} className={`sin-pdot ${etape >= (n as Etape) ? 'on' : ''}`} />
        ))}
      </div>

      {etape === 1 && (
        <form onSubmit={versPieces}>
          <div className="sec-head"><h3>Détails du sinistre</h3></div>
          {erreur && <p className="bloc-msg" style={{ marginBottom: 10 }}>{erreur}</p>}
          <div className="grid3">
            <div className="field"><label>Date <span className="req">*</span></label><input type="date" value={dateSinistre} onChange={(e) => setDate(e.target.value)} /></div>
            <div className="field"><label>Heure <span className="req">*</span></label><input type="time" value={heureSinistre} onChange={(e) => setHeure(e.target.value)} /></div>
            <div className="field"><label>Téléphone assuré</label><input value={telAssure} placeholder="0550 …" onChange={(e) => setTel(e.target.value)} /></div>
            <div className="field full"><label>Lieu du sinistre <span className="req">*</span></label><input value={lieuSinistre} placeholder="Ex. RN5, Rouiba" onChange={(e) => setLieu(e.target.value)} /></div>
          </div>
          <div className="field full" style={{ marginTop: 14 }}>
            <label>Circonstances <span className="req">*</span></label>
            <textarea value={observations} placeholder="Décrivez les circonstances…" onChange={(e) => setObs(e.target.value)} />
          </div>
          <div className="grid2" style={{ marginTop: 14 }}>
            <div className="field"><label>Compagnie adverse <span className="opt">— si tiers</span></label><input value={compagnieAdverse} onChange={(e) => setCompagnie(e.target.value)} /></div>
            <div className="field"><label>Véhicule adverse <span className="opt">— si tiers</span></label><input value={vehiculeAdverse} onChange={(e) => setVehAdverse(e.target.value)} /></div>
          </div>
          <label className="checkline" style={{ marginTop: 14 }}>
            <input type="checkbox" checked={blesses} onChange={(e) => setBlesses(e.target.checked)} /> Blessés à déclarer
          </label>
          <div className="form-actions"><button type="submit" className="btn-primary">Continuer vers les photos</button></div>
        </form>
      )}

      {etape === 2 && (
        <div>
          <div className="sec-head"><h3>Photos &amp; documents</h3><span className="ro-tag">photo ou galerie · les pièces requises bloquent la validation</span></div>
          <PiecesCapture catalogue={catalogueDeclaration} contexte={{ tiers }} pieces={pieces} layout="grille" onAjouter={ajouter} onSupprimer={supprimer} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(1)}>Retour</button>
            <button className="btn-primary" onClick={() => setEtape(3)}>Contrôler et valider</button>
          </div>
        </div>
      )}

      {etape === 3 && (
        <div>
          <div className="sec-head"><h3>Contrôle &amp; validation</h3></div>
          <ApercusControle catalogue={catalogueDeclaration} contexte={{ tiers }} pieces={pieces} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(2)}>Retour aux pièces</button>
            <button className="btn-primary" disabled={!peutEnvoyer({ pieces, compagnieAdverse, vehiculeAdverse })} onClick={valider}>
              Valider → PROASSUR
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
