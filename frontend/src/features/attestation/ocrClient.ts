// Client OCR : POST /api/attestations/lire (multipart « photo », session same-origin).
// L'OCR est MUTUALISÉ dans le conteneur reco côté back — ici on ne fait que poster la photo.
// Tout échec (réseau, HTTP, JSON) → null : l'appelant laisse la ligne « à lire » en file.

import { ContratLecture } from './types';

export async function lireAttestation(photo: Blob): Promise<ContratLecture | null> {
  try {
    const donnees = new FormData();
    donnees.append('photo', photo, 'attestation.jpg');
    const reponse = await fetch('/api/attestations/lire', {
      method: 'POST',
      credentials: 'same-origin',
      body: donnees,
    });
    if (!reponse.ok) return null;
    return (await reponse.json()) as ContratLecture;
  } catch {
    return null; // hors-ligne / réseau — la file OCR retentera
  }
}
