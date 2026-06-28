import { useEffect, useMemo, useState } from 'react';
import { PieceCapturee } from './catalogue';
import './sinistre-ui.css';

const CHECK = 'M5 12l5 5L20 6';
const WARN = 'M10.3 3.9l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01';
const INFO = 'M12 16v-5M12 8h.01M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z';

type Statut = 'CONFORME' | 'NON_CONFORME' | 'NON_LUE' | 'PAS_UN_VEHICULE' | 'VUE_SANS_PLAQUE';

interface Resultat {
  statut: Statut;
  plaqueLue: string | null;
  typeVehicule: string | null;
  confiance: number;
  estVehicule: boolean;
  bloquant: boolean;
}

/**
 * Face porteuse de plaque avec un aperçu exploitable : avant prioritaire, puis arrière. Les types de
 * pièces des deux catalogues (`face_avant`/`veh_avant`, `face_arriere`/`veh_arriere`) sont reconnus
 * par suffixe ; les autres vues (latérales, toit, diagonales) ne portent pas de plaque.
 */
function faceAvecPlaque(pieces: PieceCapturee[]): { dataUrl: string; vue: 'avant' | 'arriere' } | null {
  const exploitable = (p: PieceCapturee) => Boolean(p.dataUrl) && !p.estPdf;
  const avant = pieces.find((p) => exploitable(p) && p.type.endsWith('avant'));
  if (avant?.dataUrl) return { dataUrl: avant.dataUrl, vue: 'avant' };
  const arriere = pieces.find((p) => exploitable(p) && p.type.endsWith('arriere'));
  if (arriere?.dataUrl) return { dataUrl: arriere.dataUrl, vue: 'arriere' };
  return null;
}

function dataUrlVersBlob(dataUrl: string): Blob {
  const [entete, base64] = dataUrl.split(',');
  const mime = /:(.*?);/.exec(entete)?.[1] ?? 'image/jpeg';
  const binaire = atob(base64);
  const octets = new Uint8Array(binaire.length);
  for (let i = 0; i < binaire.length; i += 1) octets[i] = binaire.charCodeAt(i);
  return new Blob([octets], { type: mime });
}

async function analyser(dataUrl: string, vue: string, immatriculation: string): Promise<Resultat> {
  const form = new FormData();
  form.append('photo', dataUrlVersBlob(dataUrl), 'photo.jpg');
  form.append('vue', vue);
  form.append('immatriculation', immatriculation);
  const reponse = await fetch('/api/reconnaissance/analyser', {
    method: 'POST',
    credentials: 'same-origin',
    body: form,
  });
  if (!reponse.ok) throw new Error(`HTTP ${reponse.status}`);
  return reponse.json() as Promise<Resultat>;
}

/**
 * Bandeau de vérification de plaque (AGA) : compare la plaque lue par le service RECO sur la face
 * avant/arrière à l'immatriculation du contrat. CONFORT — n'altère jamais la complétude du dossier ;
 * une panne du service ou une lecture incertaine ne fait qu'inviter au contrôle manuel.
 */
export default function VerificationPlaque({
  pieces,
  immatriculation,
}: {
  pieces: PieceCapturee[];
  immatriculation?: string;
}) {
  const face = useMemo(() => faceAvecPlaque(pieces), [pieces]);
  const [etat, setEtat] = useState<'chargement' | 'ok' | 'erreur'>('chargement');
  const [res, setRes] = useState<Resultat | null>(null);

  useEffect(() => {
    if (!face || !immatriculation) return;
    let annule = false;
    setEtat('chargement');
    setRes(null);
    analyser(face.dataUrl, face.vue, immatriculation)
      .then((r) => {
        if (!annule) {
          setRes(r);
          setEtat('ok');
        }
      })
      .catch(() => {
        if (!annule) setEtat('erreur');
      });
    return () => {
      annule = true;
    };
  }, [face, immatriculation]);

  // Pas de face avant/arrière exploitable ou pas d'immatriculation → pas de contrôle de plaque.
  if (!face || !immatriculation) return null;

  let variante: 'ok' | 'ko' | 'neutre' = 'neutre';
  let icone = INFO;
  let titre = 'Vérification de la plaque…';
  let sous = `Lecture en cours sur la face ${face.vue}.`;

  if (etat === 'erreur') {
    titre = 'Vérification de plaque indisponible';
    sous = 'Le service de reconnaissance n’a pas répondu — contrôle manuel.';
  } else if (etat === 'ok' && res) {
    switch (res.statut) {
      case 'CONFORME':
        variante = 'ok';
        icone = CHECK;
        titre = 'Plaque conforme au contrat';
        sous = `Plaque lue « ${res.plaqueLue} » = immatriculation ${immatriculation}.`;
        break;
      case 'NON_CONFORME':
        variante = 'ko';
        icone = WARN;
        titre = res.bloquant ? 'Plaque NON conforme — validation bloquée' : 'Plaque non conforme au contrat';
        sous = `Plaque lue « ${res.plaqueLue} » ≠ contrat ${immatriculation}. Vérifier le véhicule${
          res.bloquant ? ' (validation refusée, anti-fraude).' : '.'
        }`;
        break;
      case 'NON_LUE':
        titre = 'Plaque non lue';
        sous = `Lecture incertaine sur la face ${face.vue} — contrôle manuel de l’immatriculation.`;
        break;
      case 'PAS_UN_VEHICULE':
        titre = 'Aucun véhicule détecté';
        sous = 'La photo ne semble pas montrer un véhicule — vérifier la prise de vue.';
        break;
      default:
        return null; // VUE_SANS_PLAQUE : ne devrait pas arriver pour une face avant/arrière
    }
  }

  return (
    <div className={`sui-cb ${variante}`}>
      <span className="cb-ic">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
          <path d={icone} />
        </svg>
      </span>
      <div>
        <div className="cb-t">{titre}</div>
        <div className="cb-s">{sous}</div>
      </div>
    </div>
  );
}
