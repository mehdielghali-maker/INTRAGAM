import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  peutTransitionner,
  estModifiable,
  estVerrouille,
  estRattachable,
  peutValider,
  creerAutoEnregistrement,
  type StatutDossier,
} from '@dossier';

describe('machine à états du dossier (présentiel + brouillon)', () => {
  it('un brouillon est modifiable et jamais rattaché à PROASSUR', () => {
    expect(estModifiable('BROUILLON')).toBe(true);
    expect(estVerrouille('BROUILLON')).toBe(false);
    expect(estRattachable('BROUILLON')).toBe(false);
  });

  it('« à valider » reste modifiable (édition tant que non validé)', () => {
    expect(estModifiable('A_VALIDER')).toBe(true);
    expect(estRattachable('A_VALIDER')).toBe(false);
  });

  it('« validée » est verrouillé et seul statut rattaché', () => {
    expect(estModifiable('VALIDEE')).toBe(false);
    expect(estVerrouille('VALIDEE')).toBe(true);
    expect(estRattachable('VALIDEE')).toBe(true);
  });

  it('validation EXIGE la complétude ; le brouillon ne la pré-suppose jamais', () => {
    expect(peutValider('BROUILLON', false)).toBe(false); // incomplet → pas validable…
    expect(peutValider('BROUILLON', true)).toBe(true); // …mais enregistrable en brouillon quand même
    expect(peutValider('A_VALIDER', true)).toBe(true);
    expect(peutValider('A_VALIDER', false)).toBe(false);
    // On ne valide pas en attente du client.
    expect(peutValider('RELANCE', true)).toBe(false);
    expect(peutValider('LIEN_ENVOYE', true)).toBe(false);
  });

  it('transitions présentiel et distant', () => {
    expect(peutTransitionner('BROUILLON', 'A_VALIDER')).toBe(true);
    expect(peutTransitionner('BROUILLON', 'VALIDEE')).toBe(true); // validation directe d'un brouillon complet
    expect(peutTransitionner('A_VALIDER', 'RELANCE')).toBe(true); // renvoyer au client
    expect(peutTransitionner('LIEN_ENVOYE', 'A_VALIDER')).toBe(true); // soumission client
    expect(peutTransitionner('RELANCE', 'A_VALIDER')).toBe(true);
    expect(peutTransitionner('VALIDEE', 'BROUILLON')).toBe(false); // verrouillé
  });
});

describe('auto-enregistrement débouncé (anti-perte)', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('n’écrit qu’une fois après le délai, avec la dernière valeur', () => {
    const enregistrer = vi.fn();
    const auto = creerAutoEnregistrement<{ statut: StatutDossier; n: number }>(enregistrer, 800);
    auto.planifier({ statut: 'BROUILLON', n: 1 });
    auto.planifier({ statut: 'BROUILLON', n: 2 });
    auto.planifier({ statut: 'BROUILLON', n: 3 });
    expect(enregistrer).not.toHaveBeenCalled(); // débounce : rien avant le délai
    vi.advanceTimersByTime(800);
    expect(enregistrer).toHaveBeenCalledTimes(1);
    expect(enregistrer).toHaveBeenCalledWith({ statut: 'BROUILLON', n: 3 });
  });

  it('flush() force la sauvegarde en attente immédiatement', () => {
    const enregistrer = vi.fn();
    const auto = creerAutoEnregistrement<number>(enregistrer, 800);
    auto.planifier(42);
    auto.flush();
    expect(enregistrer).toHaveBeenCalledWith(42);
  });

  it('annuler() jette la sauvegarde en attente', () => {
    const enregistrer = vi.fn();
    const auto = creerAutoEnregistrement<number>(enregistrer, 800);
    auto.planifier(7);
    auto.annuler();
    vi.advanceTimersByTime(2000);
    expect(enregistrer).not.toHaveBeenCalled();
  });
});
