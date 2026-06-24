/**
 * Compression d'image côté client (canvas, sans dépendance) avant « upload ».
 * Réduit le poids des photos prises au téléphone. Les PDF sont laissés tels quels.
 * Réglages surchargeables via la config (passés en paramètres).
 */
export interface FichierLocal {
  id: string;
  name: string;
  dataUrl?: string;
  isPdf: boolean;
  sizeKo: number;
}

function identifiant(): string {
  // crypto.randomUUID dispo dans les navigateurs modernes ; repli simple sinon.
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `f-${Date.now()}-${Math.round(Math.random() * 1e6)}`;
}

export async function preparerFichier(
  file: File,
  maxDimension = 1280,
  qualite = 0.7,
): Promise<FichierLocal> {
  if (file.type === 'application/pdf') {
    return { id: identifiant(), name: file.name, isPdf: true, sizeKo: Math.round(file.size / 1024) };
  }
  const dataUrl = await compresserImage(file, maxDimension, qualite);
  const sizeKo = Math.round((dataUrl.length * 3) / 4 / 1024); // taille approx. du base64
  return { id: identifiant(), name: file.name, dataUrl, isPdf: false, sizeKo };
}

function compresserImage(file: File, maxDimension: number, qualite: number): Promise<string> {
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
      resolve(canvas.toDataURL('image/jpeg', qualite));
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("Image illisible"));
    };
    img.src = url;
  });
}
