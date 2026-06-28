// Préparation générique d'une pièce (compression image canvas + métadonnées). Neutre (type:string),
// utilisée par le socle de capture pour tous les domaines (déclaration, souscription…).

import { PieceCapturee } from './catalogue';

export function identifiant(prefixe = 'p'): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${prefixe}-${Date.now()}-${Math.round(Math.random() * 1e6)}`;
}

export interface PiecePreparee {
  piece: PieceCapturee;
  blob: Blob;
}

export async function preparerPiece(
  file: File,
  type: string,
  maxDimension = 1280,
  qualite = 0.7,
): Promise<PiecePreparee> {
  const id = identifiant();
  if (file.type === 'application/pdf') {
    return { piece: { id, type, nom: file.name, estPdf: true, tailleKo: Math.round(file.size / 1024) }, blob: file };
  }
  const blob = await compresserImage(file, maxDimension, qualite);
  const dataUrl = await blobVersDataUrl(blob);
  return { piece: { id, type, nom: file.name, dataUrl, estPdf: false, tailleKo: Math.round(blob.size / 1024) }, blob };
}

function compresserImage(file: File, maxDimension: number, qualite: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      const ratio = Math.min(1, maxDimension / Math.max(img.width, img.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.round(img.width * ratio);
      canvas.height = Math.round(img.height * ratio);
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        URL.revokeObjectURL(url);
        reject(new Error('Canvas indisponible'));
        return;
      }
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(url);
      canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('Compression échouée'))), 'image/jpeg', qualite);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('Image illisible'));
    };
    img.src = url;
  });
}

function blobVersDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(new Error('Lecture impossible'));
    reader.readAsDataURL(blob);
  });
}
