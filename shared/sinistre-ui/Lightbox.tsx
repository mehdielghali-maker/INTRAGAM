// Agrandissement d'une vignette (aperçu plein écran).
export default function Lightbox({ src, onClose }: { src: string; onClose: () => void }) {
  return (
    <div className="sui-lightbox" role="dialog" aria-modal="true" onClick={onClose}>
      <button className="fermer" onClick={onClose} aria-label="Fermer">×</button>
      <img src={src} alt="Aperçu" onClick={(e) => e.stopPropagation()} />
    </div>
  );
}
