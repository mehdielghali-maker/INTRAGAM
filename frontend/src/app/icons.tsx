// Jeu d'icônes (style trait, hérite de currentColor). Équivalents propres et cohérents
// des pictos de la maquette.

export type IconName =
  | 'accueil'
  | 'depot'
  | 'attestations'
  | 'cheques'
  | 'bureau'
  | 'cotation'
  | 'expertise'
  | 'accords'
  | 'echeanciers'
  | 'contentieux'
  | 'ca-ytd'
  | 'ca-mois'
  | 'ecart'
  | 'creances'
  | 'sp'
  | 'chevron'
  | 'fleche-haut'
  | 'fleche-bas';

const PATHS: Record<IconName, string> = {
  accueil: 'M3 11l9-8 9 8M5 10v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V10',
  depot: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M9 13h6M9 17h6',
  attestations: 'M12 15a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM8.2 13.5L7 22l5-3 5 3-1.2-8.5',
  cheques: 'M2 6h20v12H2zM2 10h20M6 15h4',
  bureau: 'M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z',
  cotation: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M12 11v6M9 14h6',
  expertise: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM21 21l-4.3-4.3',
  accords: 'M3 4h18v18H3zM3 9h18M8 2v4M16 2v4M8 14h2M14 14h2',
  echeanciers: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
  contentieux: 'M10.3 3.9l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01',
  'ca-ytd': 'M23 6l-9.5 9.5-5-5L1 18M17 6h6v6',
  'ca-mois': 'M3 4h18v18H3zM3 9h18M8 2v4M16 2v4',
  ecart: 'M10.3 3.9l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3.1l-8-14a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01',
  creances: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2',
  sp: 'M22 12h-4l-3 9L9 3l-3 9H2',
  chevron: 'M9 18l6-6-6-6',
  'fleche-haut': 'M12 19V5M5 12l7-7 7 7',
  'fleche-bas': 'M12 5v14M19 12l-7 7-7-7',
};

export function Icon({ name, className }: { name: IconName; className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}
