import { useEffect, useState } from 'react';

/**
 * Détecte un TÉLÉPHONE / TABLETTE (appareil tactile) — là où la caméra in-app a du sens. Sur
 * ORDINATEUR on n'affiche que l'import de fichier.
 *
 * Détection ROBUSTE (le simple `(pointer: coarse) and (hover: none)` échoue sur certains appareils,
 * ex. Samsung avec S-Pen : le stylet rapporte une capacité de SURVOL, ce qui faisait passer le
 * téléphone pour un ordinateur). On considère mobile si le pointeur PRINCIPAL est grossier (tactile),
 * OU — repli — si l'appareil est tactile ET a un petit écran (≤ 1024px), ce qui couvre les téléphones
 * dont le media-query pointeur est faussé sans attraper les PC tactiles (grand écran + pointeur fin).
 */
function estMobileMaintenant(): boolean {
  if (typeof window === 'undefined') return false;
  const pointeurGrossier = window.matchMedia('(pointer: coarse)').matches;
  const tactilePetitEcran =
    (navigator.maxTouchPoints || 0) > 0 && window.matchMedia('(max-width: 1024px)').matches;
  return pointeurGrossier || tactilePetitEcran;
}

export function useEstMobile(): boolean {
  const [mobile, setMobile] = useState(estMobileMaintenant);
  useEffect(() => {
    const reevaluer = () => setMobile(estMobileMaintenant());
    const mqs = [window.matchMedia('(pointer: coarse)'), window.matchMedia('(max-width: 1024px)')];
    mqs.forEach((mq) => mq.addEventListener?.('change', reevaluer));
    window.addEventListener('resize', reevaluer);
    return () => {
      mqs.forEach((mq) => mq.removeEventListener?.('change', reevaluer));
      window.removeEventListener('resize', reevaluer);
    };
  }, []);
  return mobile;
}
