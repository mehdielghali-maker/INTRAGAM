// Mécanisme de brouillon partagé : persistance abstraite + auto-enregistrement débouncé.
// Le socle fournit le MÉCANISME ; chaque surface fournit le STOCKAGE concret (localStorage côté
// poste mock, Dexie côté PWA) en implémentant BrouillonStore. Aucune dépendance framework.

/**
 * Stockage local d'un dossier en cours (brouillon). Upsert idempotent par `idLocal`. Ces opérations
 * NE TOUCHENT JAMAIS le backend de référence (DECSIN/SecGam/PROASSUR) : un brouillon reste LOCAL
 * jusqu'à la validation.
 */
export interface BrouillonStore<T> {
  lister(): Promise<T[]>;
  obtenir(idLocal: string): Promise<T | null>;
  enregistrer(dossier: T): Promise<T>;
  supprimer(idLocal: string): Promise<void>;
}

export interface AutoEnregistrement<T> {
  /** À appeler à chaque modification (champ ou pièce) : programme une sauvegarde différée. */
  planifier(dossier: T): void;
  /** Force la sauvegarde en attente immédiatement (ex. au démontage / avant navigation). */
  flush(): void;
  /** Annule une sauvegarde en attente sans l'exécuter. */
  annuler(): void;
}

/**
 * Auto-enregistrement DÉBOUNCÉ (anti-perte) : la sauvegarde réelle (`enregistrer`) s'exécute après
 * `delaiMs` sans nouvelle modification. Garantit qu'aucune saisie (champs ET pièces) n'est perdue,
 * sans écrire à chaque frappe. Indépendant de React (les composants l'enveloppent dans un ref).
 */
export function creerAutoEnregistrement<T>(
  enregistrer: (dossier: T) => void | Promise<void>,
  delaiMs = 800,
): AutoEnregistrement<T> {
  let timer: ReturnType<typeof setTimeout> | null = null;
  let enAttente: T | null = null;

  const executer = () => {
    timer = null;
    if (enAttente !== null) {
      const aSauver = enAttente;
      enAttente = null;
      void enregistrer(aSauver);
    }
  };

  return {
    planifier(dossier: T) {
      enAttente = dossier;
      if (timer) clearTimeout(timer);
      timer = setTimeout(executer, delaiMs);
    },
    flush() {
      if (timer) {
        clearTimeout(timer);
        executer();
      }
    },
    annuler() {
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      enAttente = null;
    },
  };
}
