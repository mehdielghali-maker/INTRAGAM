import { useEffect, useState } from 'react';

const REQUETE = '(pointer: coarse) and (hover: none)';

/**
 * Vrai sur un appareil TACTILE sans survol (téléphone / tablette) — là où la caméra a du sens.
 * Sur ORDINATEUR (pointeur fin + survol), la webcam est inutile pour photographier un véhicule ou
 * un document : on n'affiche alors que l'import de fichier. Réagit aux changements (ex. branchement
 * d'un écran tactile) via le listener matchMedia.
 */
export function useEstMobile(): boolean {
  const [mobile, setMobile] = useState(
    () => typeof window !== 'undefined' && window.matchMedia(REQUETE).matches,
  );
  useEffect(() => {
    const mq = window.matchMedia(REQUETE);
    const onChange = () => setMobile(mq.matches);
    mq.addEventListener?.('change', onChange);
    return () => mq.removeEventListener?.('change', onChange);
  }, []);
  return mobile;
}
