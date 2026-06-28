import { FormEvent, useEffect, useRef, useState } from 'react';
import {
  brouillonsLocaux,
  catalogueDeclaration,
  decsin,
  Declaration,
  genererCode,
  identifiant,
  peutEnvoyer,
  piecesManquantes,
  Piece,
  Vehicule,
} from '@decsin';
import { ApercusControle, definition, PieceCapturee, PiecesCapture } from '@sinistre-ui';
import { creerAutoEnregistrement } from '@dossier';

type Etape = 1 | 2 | 3;

/**
 * Parcours de saisie AGA (PRÉSENTIEL, par défaut) : détails → pièces (socle partagé) → contrôle.
 * Le dossier est enregistrable EN BROUILLON à tout moment (même incomplet) et AUTO-ENREGISTRÉ en
 * continu (anti-perte). La VALIDATION (rattachement PROASSUR) n'est possible qu'une fois complet ;
 * elle seule envoie le dossier (un brouillon reste LOCAL). Reprise possible via la prop `brouillon`.
 */
export default function DeclarationCaptureStepper({
  vehicule,
  onTermine,
  brouillon,
}: {
  vehicule: Vehicule;
  onTermine: (message: string) => void;
  brouillon?: Declaration;
}) {
  const [etape, setEtape] = useState<Etape>(1);
  const [dateSinistre, setDate] = useState(brouillon?.dateSinistre ?? '');
  const [heureSinistre, setHeure] = useState(brouillon?.heureSinistre ?? '');
  const [lieuSinistre, setLieu] = useState(brouillon?.lieuSinistre ?? '');
  const [observations, setObs] = useState(brouillon?.observations ?? '');
  const [blesses, setBlesses] = useState(brouillon?.blesses ?? false);
  const [telAssure, setTel] = useState(brouillon?.telAssure ?? '');
  const [compagnieAdverse, setCompagnie] = useState(brouillon?.compagnieAdverse ?? '');
  const [vehiculeAdverse, setVehAdverse] = useState(brouillon?.vehiculeAdverse ?? '');
  const [pieces, setPieces] = useState<Piece[]>(brouillon?.pieces ?? []);
  const [erreur, setErreur] = useState<string | null>(null);
  const [enregistre, setEnregistre] = useState(false);

  // Identité STABLE du dossier : reprise d'un brouillon existant, ou nouvelle générée une seule fois.
  const idLocalRef = useRef(brouillon?.idLocal ?? identifiant('d'));
  const codeRef = useRef(brouillon?.code ?? genererCode());

  const tiers = Boolean(compagnieAdverse.trim() || vehiculeAdverse.trim());
  const complet = peutEnvoyer({ pieces, compagnieAdverse, vehiculeAdverse });
  const manquantes = piecesManquantes({ pieces, compagnieAdverse, vehiculeAdverse });

  function declarationCourante(statut: Declaration['statut']): Declaration {
    return {
      idLocal: idLocalRef.current,
      code: codeRef.current,
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
      dateSaisie: brouillon?.dateSaisie ?? new Date().toISOString().slice(0, 10),
    };
  }

  // Auto-enregistrement DÉBOUNCÉ (anti-perte) : sauvegarde le brouillon LOCAL à chaque modification.
  const autoRef = useRef(
    creerAutoEnregistrement<Declaration>(async (d) => {
      await brouillonsLocaux.enregistrer(d);
      setEnregistre(true);
    }, 800),
  );
  useEffect(() => {
    const aDuContenu =
      Boolean(brouillon) ||
      Boolean(
        dateSinistre || heureSinistre || lieuSinistre.trim() || observations.trim() ||
          telAssure.trim() || compagnieAdverse.trim() || vehiculeAdverse.trim() || pieces.length,
      );
    if (!aDuContenu) return; // ne crée pas un brouillon vide à l'ouverture
    setEnregistre(false);
    autoRef.current.planifier(declarationCourante('BROUILLON'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dateSinistre, heureSinistre, lieuSinistre, observations, blesses, telAssure, compagnieAdverse, vehiculeAdverse, pieces]);
  // Sauvegarde finale au démontage (navigation hors du stepper).
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

  function versPieces(e: FormEvent) {
    e.preventDefault();
    if (!dateSinistre || !heureSinistre || !lieuSinistre.trim() || !observations.trim()) {
      setErreur('Date, heure, lieu et circonstances sont obligatoires.');
      return;
    }
    setErreur(null);
    setEtape(2);
  }

  async function enregistrerBrouillon() {
    autoRef.current.annuler();
    await brouillonsLocaux.enregistrer(declarationCourante('BROUILLON'));
    onTermine(`Brouillon ${codeRef.current} enregistré — vous pourrez le reprendre depuis le suivi.`);
  }

  async function valider() {
    autoRef.current.annuler();
    if (!navigator.onLine) {
      // Capture OK hors-ligne ; rattachement PROASSUR (validation) différé à la reconnexion.
      await decsin.creerDeclaration({ ...declarationCourante('A_VALIDER'), aRattacher: true });
      await brouillonsLocaux.supprimer(idLocalRef.current);
      onTermine(`Déclaration ${codeRef.current} enregistrée hors-ligne — validée à la reconnexion.`);
      return;
    }
    const decl = await decsin.creerDeclaration(declarationCourante('A_VALIDER'));
    const { numSinistre, idDossierSinistre } = await decsin.rattacherDeclaration(decl);
    await decsin.creerDeclaration({ ...decl, statut: 'VALIDEE', numSinistre, idDossierSinistre });
    await brouillonsLocaux.supprimer(idLocalRef.current);
    onTermine(`Déclaration ${decl.code} validée — N° sinistre ${numSinistre}.`);
  }

  const boutonBrouillon = (
    <button type="button" className="btn-ghost" onClick={enregistrerBrouillon}>
      Enregistrer en brouillon
    </button>
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
          <div className="form-actions">
            {boutonBrouillon}
            <button type="submit" className="btn-primary">Continuer vers les photos</button>
          </div>
        </form>
      )}

      {etape === 2 && (
        <div>
          <div className="sec-head"><h3>Photos &amp; documents</h3><span className="ro-tag">photo ou galerie · les pièces requises bloquent la validation</span></div>
          <PiecesCapture catalogue={catalogueDeclaration} contexte={{ tiers }} pieces={pieces} layout="grille" onAjouter={ajouter} onSupprimer={supprimer} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(1)}>Retour</button>
            {boutonBrouillon}
            <button className="btn-primary" onClick={() => setEtape(3)}>Contrôler</button>
          </div>
        </div>
      )}

      {etape === 3 && (
        <div>
          <div className="sec-head"><h3>Contrôle &amp; validation</h3></div>
          <ApercusControle catalogue={catalogueDeclaration} contexte={{ tiers }} pieces={pieces} />
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setEtape(2)}>Retour aux pièces</button>
            {boutonBrouillon}
            <button className="btn-primary" disabled={!complet} onClick={valider}>
              Valider → PROASSUR
            </button>
          </div>
          {!complet && (
            <p className="hint" style={{ marginTop: 6 }}>
              Validation impossible — pièces obligatoires manquantes :{' '}
              {manquantes.map((t) => definition(catalogueDeclaration, t)?.libelle ?? t).join(', ')}.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
