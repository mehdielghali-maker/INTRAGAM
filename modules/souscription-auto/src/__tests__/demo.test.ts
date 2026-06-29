// Données de DÉMO de la souscription auto (session AGA) : seed du store mock quand vide,
// statuts variés, et IDEMPOTENCE (un re-seed ne duplique pas). On passe par l'adaptateur public
// @souscription (souscriptionMock) pour tester le comportement réel du store localStorage.
import { beforeEach, describe, expect, it } from 'vitest';
import { souscription, souscriptionsDemo } from '@souscription';

describe('souscriptions de démo (session AGA)', () => {
  beforeEach(() => {
    localStorage.clear(); // store vide → 1re visite : le mock doit semer les démos
  });

  it('souscriptionsDemo() retourne 3 souscriptions AUTO aux statuts VARIÉS', () => {
    const demo = souscriptionsDemo();
    expect(demo).toHaveLength(3);
    const statuts = demo.map((s) => s.statut).sort();
    expect(statuts).toEqual(['A_VALIDER', 'LIEN_ENVOYE', 'VALIDEE']);
    // Toutes AUTO / branche 13, champs requis présents, références déterministes.
    for (const s of demo) {
      expect(s.typeProduit).toBe('AUTO');
      expect(s.codeBranche).toBe('13');
      expect(s.idLocal).toBeTruthy();
      expect(s.reference).toMatch(/^SCR-DEMO-\d{4}$/);
      expect(s.numeroPolice).toBeTruthy();
      expect(s.nomClient).toBeTruthy();
      expect(s.assure).toBeDefined();
      expect(s.vehicule).toBeDefined();
      expect(Array.isArray(s.pieces)).toBe(true);
      expect(s.agence?.code).toBeTruthy();
    }
  });

  it('les polices de démo existent déjà dans le référentiel (recherche d\'entité)', async () => {
    for (const np of ['AUTO-2026-00123', 'AUTO-2026-00457']) {
      const trouvees = await souscription.rechercheEntite({ numeroPolice: np });
      expect(trouvees.some((e) => e.numeroPolice === np)).toBe(true);
    }
  });

  it('sème les 3 démos quand le store est vide (1re visite)', async () => {
    const liste = await souscription.getSouscriptions();
    expect(liste).toHaveLength(3);
    expect(liste.map((s) => s.statut).sort()).toEqual(['A_VALIDER', 'LIEN_ENVOYE', 'VALIDEE']);
  });

  it('le filtre par statut fonctionne sur les démos semées', async () => {
    const aValider = await souscription.getSouscriptions({ statut: 'A_VALIDER' });
    expect(aValider).toHaveLength(1);
    expect(aValider[0].reference).toBe('SCR-DEMO-0001');
  });

  it('est IDEMPOTENT : un second accès ne duplique pas (références stables)', async () => {
    const premier = await souscription.getSouscriptions();
    const refs = premier.map((s) => s.reference).sort();
    // 2e lecture (le store n'est plus vide → pas de re-seed, on relit l'existant)
    const second = await souscription.getSouscriptions();
    expect(second).toHaveLength(3);
    expect(second.map((s) => s.reference).sort()).toEqual(refs);
    // Aucune référence en double dans le store.
    expect(new Set(refs).size).toBe(3);
  });

  it('enregistrer une démo (VALIDER) ne crée pas de doublon (upsert sur reference)', async () => {
    const [premiere] = await souscription.getSouscriptions();
    await souscription.enregistrerSouscription(premiere, []);
    const apres = await souscription.getSouscriptions();
    expect(apres).toHaveLength(3); // toujours 3 : mise à jour en place, pas d'ajout
    const maj = apres.find((s) => s.reference === premiere.reference);
    expect(maj?.statut).toBe('VALIDEE');
  });
});
