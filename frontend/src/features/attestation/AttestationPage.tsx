import { ChangeEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { preparerPiece } from '@sinistre-ui';
import { useEstMobile } from '@sinistre-ui/useEstMobile';
import { useAgence } from '../../app/AgencyContext';
import { listerRelevesValides } from './api';
import {
  ajouterLigne,
  demanderPersistance,
  majLigne,
  majReleve,
  mettreEnFileOcr,
  ouvrirLot,
  lignesDuLot,
  supprimerLigne,
} from './db';
import {
  POLICE_VALIDE,
  champsVides,
  cleLot,
  compterAttestations,
  contratVersChamps,
  estComptabilisee,
  estDoublonPolice,
  formaterMontant,
  libelleMois,
  listerMoisSelectionnables,
  moisCourant,
  proposerStatutLigne,
  totalPrimeTTC,
} from './logiqueReleve';
import { activerSyncAttestations, lireLigneEnFile, traiterFileValidation } from './sync';
import { ChampsLigne, LigneReleve, ReleveLocal, StatutLigne } from './types';
import './attestation.css';

// Pictos (traits, style maquette).
const CAM = 'M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z';
const CHECK = 'M5 12l5 5L20 6';
const WARN =
  'M12 9v4M12 17h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z';
const PEN = 'M17 3a2.8 2.8 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z';
const CROSS = 'M18 6 6 18M6 6l12 12';

/** Libellés des chips de statut de ligne (les 2 de la maquette + les 2 états de lecture). */
const CHIPS: Record<StatutLigne, { classe: string; libelle: string }> = {
  confirme: { classe: 'att-chip-ok', libelle: 'Confirmé' },
  a_verifier: { classe: 'att-chip-warn', libelle: 'À vérifier' },
  a_lire: { classe: 'att-chip-wait', libelle: 'À lire' },
  lu: { classe: 'att-chip-lu', libelle: 'Lu · à confirmer' },
};

/** État du panneau OCR assisté (pré-rempli, éditable). */
interface PanneauEtat {
  idLigne: string;
  champs: ChampsLigne;
  confiance?: number;
  statutContrat: 'lu' | 'a_verifier';
  /** « Corriger » activé → grille de champs en saisie. */
  enCorrection: boolean;
  /** Ouvert depuis une ligne déjà ajoutée (édition / re-scan). */
  modeEdition: boolean;
  photoDataUrl?: string;
}

const CLES_CHAMPS: { cle: keyof ChampsLigne; libelle: string; mono?: boolean }[] = [
  { cle: 'numeroPolice', libelle: 'N° de police', mono: true },
  { cle: 'numeroQuittance', libelle: 'N° de quittance', mono: true },
  { cle: 'immatriculation', libelle: 'Immatriculation' },
  { cle: 'assure', libelle: 'Assuré' },
  { cle: 'valideDu', libelle: 'Valide du' },
  { cle: 'valideAu', libelle: 'Valide au' },
  { cle: 'primeTTC', libelle: 'Prime TTC' },
  { cle: 'codeAgence', libelle: 'Agence' },
];

/**
 * Attestations — relevé mensuel de production. COLLECTE seule : l'AGA photographie chaque
 * attestation, l'OCR (mutualisé côté back) pré-remplit, l'AGA confirme/corrige, puis valide le
 * relevé du mois (un lot par agence active + mois). Hors-ligne : files OCR + validation (Dexie).
 */
export default function AttestationPage() {
  const { contexte } = useAgence();
  const consolide = contexte?.consolideActif ?? false;
  const agence = contexte?.agenceActive?.code;
  const mobile = useEstMobile();

  const [mois, setMois] = useState(moisCourant());
  const [releve, setReleve] = useState<ReleveLocal | null>(null);
  const [lignes, setLignes] = useState<LigneReleve[]>([]);
  const [panneau, setPanneau] = useState<PanneauEtat | null>(null);
  const [lectureEnCours, setLectureEnCours] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [flashId, setFlashId] = useState<string | null>(null);

  const moisChoix = useMemo(() => listerMoisSelectionnables(new Date(), 6), []);
  const verrouille = releve !== null && releve.statut !== 'BROUILLON';
  const nbAjoutees = compterAttestations(lignes);
  const total = totalPrimeTTC(lignes);
  const enAttente = lignes.filter((l) => !estComptabilisee(l.statutLigne)).length;

  /** Recharge le lot (agence active | mois) : réouverture, JAMAIS de doublon (clé unique). */
  const recharger = useCallback(async () => {
    if (!agence || consolide) return;
    setReleve(await ouvrirLot(agence, mois));
    setLignes(await lignesDuLot(agence, mois));
  }, [agence, mois, consolide]);

  useEffect(() => {
    setPanneau(null);
    setMessage(null);
    void recharger();
  }, [recharger]);

  // Persistance du stockage (photos en file) + files OCR/validation : au chargement et au retour réseau.
  useEffect(() => {
    void demanderPersistance();
    return activerSyncAttestations(() => void recharger());
  }, [recharger]);

  // Réconciliation serveur : (agence, mois) déjà validé côté back → verrou local aussi (409 évité).
  useEffect(() => {
    if (!agence || consolide) return;
    let actif = true;
    listerRelevesValides()
      .then(async (valides) => {
        const distant = valides.find((r) => r.codeAgence === agence && r.mois === mois);
        if (!distant || !actif) return;
        const lot = await ouvrirLot(agence, mois);
        if (lot.statut !== 'VALIDEE') {
          await majReleve(cleLot(agence, mois), {
            statut: 'VALIDEE',
            reference: distant.reference,
            dateValidation: distant.dateValidation,
          });
          await recharger();
        }
      })
      .catch(() => undefined); // hors-ligne : silencieux, le lot local fait foi
    return () => {
      actif = false;
    };
  }, [agence, mois, consolide, recharger]);

  // Animation « flash » de la ligne fraîchement ajoutée (maquette).
  useEffect(() => {
    if (!flashId) return;
    const t = window.setTimeout(() => setFlashId(null), 1200);
    return () => window.clearTimeout(t);
  }, [flashId]);

  /** Photo prise / image importée : ligne créée IMMÉDIATEMENT ('a_lire'), puis tentative OCR. */
  async function surPhoto(e: ChangeEvent<HTMLInputElement>) {
    const fichier = e.target.files?.[0];
    e.target.value = '';
    if (!fichier || !agence || verrouille || lectureEnCours) return;
    setMessage(null);
    setLectureEnCours(true);
    try {
      const { piece, blob } = await preparerPiece(fichier, 'attestation');
      if (piece.estPdf) {
        setMessage('Choisissez une image (photo de l’attestation), pas un PDF.');
        return;
      }
      const ligne: LigneReleve = {
        id: piece.id,
        mois,
        agence,
        ...champsVides(),
        statutLigne: 'a_lire',
        photoDataUrl: piece.dataUrl,
        dateAjout: Date.now(),
      };
      await ajouterLigne(ligne);
      await mettreEnFileOcr(ligne.id, blob);
      await recharger(); // la ligne apparaît tout de suite dans le tableau
      const contrat = await lireLigneEnFile(ligne.id);
      await recharger();
      if (contrat) {
        setPanneau({
          idLigne: ligne.id,
          champs: contratVersChamps(contrat),
          confiance: contrat.confiance,
          statutContrat: contrat.statut,
          enCorrection: false,
          modeEdition: false,
          photoDataUrl: piece.dataUrl,
        });
      } else {
        setMessage(
          'Lecture impossible pour le moment (hors-ligne ?) — la photo est en file, la ligne reste « à lire » et sera lue au retour du réseau.',
        );
      }
    } finally {
      setLectureEnCours(false);
    }
  }

  /** Re-scan depuis le panneau d'édition : nouvelle photo → la ligne repart en lecture. */
  async function surRescan(e: ChangeEvent<HTMLInputElement>) {
    const fichier = e.target.files?.[0];
    e.target.value = '';
    if (!fichier || !panneau || verrouille || lectureEnCours) return;
    setLectureEnCours(true);
    try {
      const { piece, blob } = await preparerPiece(fichier, 'attestation');
      if (piece.estPdf) {
        setMessage('Choisissez une image (photo de l’attestation), pas un PDF.');
        return;
      }
      await majLigne(panneau.idLigne, { photoDataUrl: piece.dataUrl, statutLigne: 'a_lire' });
      await mettreEnFileOcr(panneau.idLigne, blob);
      await recharger();
      const contrat = await lireLigneEnFile(panneau.idLigne);
      await recharger();
      if (contrat) {
        setPanneau((p) =>
          p && {
            ...p,
            champs: contratVersChamps(contrat),
            confiance: contrat.confiance,
            statutContrat: contrat.statut,
            enCorrection: false,
            photoDataUrl: piece.dataUrl,
          },
        );
      } else {
        setPanneau(null);
        setMessage('Nouvelle photo en file : elle sera lue au retour du réseau.');
      }
    } finally {
      setLectureEnCours(false);
    }
  }

  /** Ajout au relevé : garde-fou DOUBLON de n° de police (confirmation pour forcer). */
  async function ajouterAuReleve() {
    if (!panneau || verrouille) return;
    const { champs } = panneau;
    if (estDoublonPolice(lignes, champs.numeroPolice, panneau.idLigne)) {
      const forcer = window.confirm(
        `Le n° de police ${champs.numeroPolice} figure déjà dans le relevé de ${libelleMois(mois)}.\nAjouter quand même cette attestation ?`,
      );
      if (!forcer) return;
    }
    const statut = proposerStatutLigne(champs, panneau.statutContrat, panneau.enCorrection);
    await majLigne(panneau.idLigne, { ...champs, statutLigne: statut, confiance: panneau.confiance });
    setPanneau(null);
    setFlashId(panneau.idLigne);
    await recharger();
  }

  /** Ré-ouvre une ligne déjà ajoutée (édition, vignette photo, re-scan possible). */
  function editerLigne(l: LigneReleve) {
    if (verrouille) return;
    setPanneau({
      idLigne: l.id,
      champs: {
        numeroPolice: l.numeroPolice,
        numeroQuittance: l.numeroQuittance,
        immatriculation: l.immatriculation,
        assure: l.assure,
        valideDu: l.valideDu,
        valideAu: l.valideAu,
        primeTTC: l.primeTTC,
        codeAgence: l.codeAgence,
      },
      confiance: l.confiance,
      // Une ligne jamais lue ou douteuse reste « à vérifier » tant que l'AGA ne corrige pas.
      statutContrat: l.statutLigne === 'confirme' || l.statutLigne === 'lu' ? 'lu' : 'a_verifier',
      enCorrection: true,
      modeEdition: true,
      photoDataUrl: l.photoDataUrl,
    });
  }

  async function supprimer(l: LigneReleve) {
    if (verrouille) return;
    if (!window.confirm('Supprimer cette attestation du relevé ?')) return;
    await supprimerLigne(l.id);
    if (panneau?.idLigne === l.id) setPanneau(null);
    await recharger();
  }

  /** Validation du relevé du mois : verrouille le lot ; hors-ligne → file de validation. */
  async function validerMois() {
    if (!releve || verrouille) return;
    if (nbAjoutees === 0) {
      window.alert('Aucune attestation dans ce relevé : photographiez-en au moins une.');
      return;
    }
    if (enAttente > 0) {
      window.alert(
        `${enAttente} attestation(s) encore en lecture ou à confirmer.\nConfirmez-les (ou supprimez-les) avant de valider le relevé.`,
      );
      return;
    }
    const ok = window.confirm(
      `Valider le relevé de ${libelleMois(mois)} (${nbAjoutees} attestation${nbAjoutees > 1 ? 's' : ''}) ?\nIl sera ensuite verrouillé (lecture seule).`,
    );
    if (!ok) return;
    await majReleve(releve.cle, { statut: 'A_VALIDER' });
    await traiterFileValidation(); // en ligne : part tout de suite → VALIDEE ; sinon file
    await recharger();
  }

  function majChamp(cle: keyof ChampsLigne, valeur: string) {
    setPanneau((p) => (p ? { ...p, champs: { ...p.champs, [cle]: valeur } } : p));
  }

  // ----- Rendus -----

  if (consolide) {
    return (
      <section className="attestation">
        <h1 className="page-h">Attestations — relevé mensuel de production</h1>
        <p className="page-sub">
          Photographiez chaque attestation : l'OCR la lit, vous confirmez, le relevé du mois se
          constitue au fil de l'eau.
        </p>
        <div className="conso-banner">
          Mode consolidé (vue d'ensemble) : le relevé d'attestations concerne une agence précise.
          Sélectionnez une agence dans le commutateur en haut à droite pour photographier ou
          valider un relevé.
        </div>
      </section>
    );
  }

  const panneauWarn =
    panneau !== null &&
    (panneau.statutContrat === 'a_verifier' || !POLICE_VALIDE.test(panneau.champs.numeroPolice.trim()));

  return (
    <section className="attestation">
      <h1 className="page-h">Attestations — relevé mensuel de production</h1>
      <p className="page-sub">
        Photographiez chaque attestation : l'OCR la lit, vous confirmez ou corrigez, et le relevé de
        l'agence <b>{contexte?.agenceActive?.nom ?? '…'}</b> ({agence ?? '…'}) se constitue au fil de
        l'eau — un relevé par mois, validé une seule fois.
      </p>

      <div className="att-colonne">
        {/* Barre de mois (fond vert foncé, sélecteur or) */}
        <div className="att-month-bar">
          <div>
            <div className="att-ml">Relevé du mois</div>
            <div className="att-mv">{libelleMois(mois)}</div>
          </div>
          <select
            aria-label="Choisir le mois"
            value={mois}
            onChange={(e) => setMois(e.target.value)}
          >
            {moisChoix.map((m) => (
              <option key={m.valeur} value={m.valeur}>
                {m.libelle}
              </option>
            ))}
          </select>
        </div>

        {/* Relevé verrouillé (validé, ou validation en attente de réseau) */}
        {verrouille && (
          <div className={`att-bandeau ${releve?.statut === 'VALIDEE' ? 'ok' : 'attente'}`}>
            {releve?.statut === 'VALIDEE' ? (
              <>
                Relevé validé
                {releve.dateValidation
                  ? ` le ${new Date(releve.dateValidation).toLocaleDateString('fr-FR')}`
                  : ''}
                {releve.reference ? (
                  <>
                    {' '}
                    — réf <b>{releve.reference}</b>
                  </>
                ) : (
                  ' (déjà validé côté serveur)'
                )}
                . Lecture seule.
              </>
            ) : (
              <>Validation demandée hors-ligne : envoi automatique au retour du réseau. Lecture seule.</>
            )}
          </div>
        )}

        {!verrouille && (
          <>
            <div className="att-s-title">Photographier une attestation</div>
            <div className="att-s-sub">
              Chaque attestation lue est ajoutée au relevé de <b>{libelleMois(mois)}</b>.
            </div>

            {/* Zone capture : caméra sur mobile, import de fichier sur ordinateur */}
            <div className="att-capture">
              <div className="att-cam">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.7}>
                  <path d={CAM} />
                  <circle cx="12" cy="13" r="4" />
                </svg>
              </div>
              <p>
                {mobile
                  ? "Cadrez l'attestation, prenez la photo — l'OCR lit le document."
                  : "Importez la photo de l'attestation — l'OCR lit le document."}
              </p>
              {mobile ? (
                <>
                  <label className={`att-btn att-btn-p${lectureEnCours ? ' off' : ''}`}>
                    {lectureEnCours ? 'Lecture OCR en cours…' : 'Prendre en photo une attestation'}
                    <input
                      type="file"
                      accept="image/*"
                      capture="environment"
                      disabled={lectureEnCours}
                      onChange={surPhoto}
                    />
                  </label>
                  <div className="att-btn-row">
                    <label className={`att-btn att-btn-g att-btn-sm${lectureEnCours ? ' off' : ''}`}>
                      Importer une image
                      <input type="file" accept="image/*" disabled={lectureEnCours} onChange={surPhoto} />
                    </label>
                  </div>
                </>
              ) : (
                <label className={`att-btn att-btn-p${lectureEnCours ? ' off' : ''}`}>
                  {lectureEnCours ? 'Lecture OCR en cours…' : 'Importer une image'}
                  <input type="file" accept="image/*" disabled={lectureEnCours} onChange={surPhoto} />
                </label>
              )}
            </div>

            {message && <div className="att-message">{message}</div>}

            {/* Panneau OCR assisté (pré-rempli, éditable) */}
            {panneau && (
              <div className={`att-ocr${panneauWarn ? ' warn' : ''}`}>
                <div className="att-ocr-h">
                  <span className="ic">
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth={panneauWarn ? 2.2 : 2.4}
                    >
                      <path d={panneauWarn ? WARN : CHECK} />
                    </svg>
                  </span>
                  <div>
                    <div className="t">
                      {panneau.modeEdition
                        ? 'Modifier la ligne'
                        : panneauWarn
                          ? 'À vérifier'
                          : 'Attestation lue'}
                    </div>
                    <div className="s">
                      {panneauWarn
                        ? 'Lecture incertaine — corrigez avant l’ajout'
                        : 'Vérifiez puis ajoutez au relevé'}
                    </div>
                  </div>
                  {panneau.confiance !== undefined && (
                    <span className="conf">Confiance {Math.round(panneau.confiance * 100)} %</span>
                  )}
                </div>

                {panneau.photoDataUrl && (panneau.enCorrection || panneau.modeEdition) && (
                  <div className="att-vignette">
                    <img src={panneau.photoDataUrl} alt="Photo de l'attestation" />
                    <label className={`att-btn att-btn-g att-btn-sm${lectureEnCours ? ' off' : ''}`}>
                      {lectureEnCours ? 'Lecture…' : 'Reprendre la photo'}
                      <input
                        type="file"
                        accept="image/*"
                        {...(mobile ? { capture: 'environment' as const } : {})}
                        disabled={lectureEnCours}
                        onChange={surRescan}
                      />
                    </label>
                  </div>
                )}

                <div className="att-ex">
                  {CLES_CHAMPS.map(({ cle, libelle, mono }) => (
                    <FragmentChamp
                      key={cle}
                      libelle={libelle}
                      mono={mono}
                      valeur={panneau.champs[cle]}
                      enCorrection={panneau.enCorrection}
                      douteux={cle === 'numeroPolice' && panneauWarn}
                      onChange={(v) => majChamp(cle, v)}
                    />
                  ))}
                </div>

                <div className="att-ocr-note">
                  <span>
                    {panneauWarn
                      ? 'Le n° de police lu n’a pas 15 chiffres (ou la lecture est douteuse). Corrigez-le avant l’ajout, ou ajoutez la ligne en « à vérifier ».'
                      : 'Lecture assistée : l’OCR pré-remplit, vous confirmez ou corrigez avant l’ajout.'}
                  </span>
                </div>

                <div className="att-btn-row">
                  {panneau.enCorrection ? (
                    <button type="button" className="att-btn att-btn-g" onClick={() => setPanneau(null)}>
                      Annuler
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="att-btn att-btn-g"
                      onClick={() => setPanneau((p) => (p ? { ...p, enCorrection: true } : p))}
                    >
                      Corriger
                    </button>
                  )}
                  <button type="button" className="att-btn att-btn-o" onClick={() => void ajouterAuReleve()}>
                    {panneau.modeEdition ? 'Enregistrer la ligne' : 'Ajouter au relevé'}
                  </button>
                </div>
              </div>
            )}
          </>
        )}

        {/* Résumé : compteur + prime TTC cumulée */}
        <div className="att-sumbar">
          <div className="c">
            <div className="n">{nbAjoutees}</div>
            <div className="l">attestation{nbAjoutees > 1 ? 's' : ''}</div>
          </div>
          <div className="c alt">
            <div className="n">{formaterMontant(total)}</div>
            <div className="l">Prime TTC cumulée (DA)</div>
          </div>
        </div>

        {/* Tableau du relevé */}
        <div className="att-tbl-wrap">
          <div className="att-tbl-cap">Relevé de {libelleMois(mois)}</div>
          <table>
            <thead>
              <tr>
                <th>N° police · véhicule</th>
                <th>N° quittance</th>
                <th className="r">Prime TTC</th>
                <th>Statut</th>
                {!verrouille && <th className="a" aria-label="Actions" />}
              </tr>
            </thead>
            <tbody>
              {lignes.length === 0 ? (
                <tr>
                  <td colSpan={verrouille ? 4 : 5} className="att-empty">
                    Aucune attestation pour ce mois.
                    <br />
                    {verrouille ? '' : 'Prenez une première photo pour commencer.'}
                  </td>
                </tr>
              ) : (
                lignes.map((l) => {
                  const chip = CHIPS[l.statutLigne];
                  const meta = [l.immatriculation, [l.valideDu, l.valideAu].filter(Boolean).join(' – '), l.codeAgence ? `Ag. ${l.codeAgence}` : '']
                    .filter(Boolean)
                    .join(' · ');
                  return (
                    <tr key={l.id} className={l.id === flashId ? 'att-flash' : undefined}>
                      <td>
                        <div className="att-pol">{l.numeroPolice || '—'}</div>
                        <div className="att-meta">{meta || 'En attente de lecture'}</div>
                      </td>
                      <td>
                        <span className="att-qn">{l.numeroQuittance || '—'}</span>
                      </td>
                      <td className="r">{l.primeTTC || '—'}</td>
                      <td>
                        <span className={`att-chip ${chip.classe}`}>{chip.libelle}</span>
                      </td>
                      {!verrouille && (
                        <td className="a">
                          <button
                            type="button"
                            className="att-act"
                            title="Modifier / re-scanner"
                            aria-label={`Modifier la ligne ${l.numeroPolice || ''}`}
                            onClick={() => editerLigne(l)}
                          >
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
                              <path d={PEN} />
                            </svg>
                          </button>
                          <button
                            type="button"
                            className="att-act rm"
                            title="Supprimer"
                            aria-label={`Supprimer la ligne ${l.numeroPolice || ''}`}
                            onClick={() => void supprimer(l)}
                          >
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8}>
                              <path d={CROSS} />
                            </svg>
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {!verrouille && (
          <>
            <button type="button" className="att-btn att-btn-p att-valider" onClick={() => void validerMois()}>
              Valider le relevé du mois
            </button>
            <div className="att-foot-note">Enregistré au fil de l'eau · brouillon jusqu'à la validation</div>
          </>
        )}
      </div>
    </section>
  );
}

/** Un champ de la grille du panneau : valeur en lecture, ou input en mode correction. */
function FragmentChamp({
  libelle,
  valeur,
  mono,
  douteux,
  enCorrection,
  onChange,
}: {
  libelle: string;
  valeur: string;
  mono?: boolean;
  douteux?: boolean;
  enCorrection: boolean;
  onChange: (v: string) => void;
}) {
  return (
    <>
      <span className="k">{libelle}</span>
      {enCorrection ? (
        <input
          className={`att-in${douteux ? ' flag' : ''}`}
          value={valeur}
          onChange={(e) => onChange(e.target.value)}
          aria-label={libelle}
        />
      ) : (
        <span className={`v${mono ? ' mono' : ''}${douteux ? ' flag' : ''}`}>{valeur || '—'}</span>
      )}
    </>
  );
}
