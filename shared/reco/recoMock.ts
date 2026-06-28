import { RecoPort } from './recoPort';

/**
 * Adapter MOCK (dev / tests, offline) : aucune dépendance réseau. Simule une lecture conforme
 * sur les vues avant/arrière (plaque de démo) et une simple détection sur les autres. Calqué sur
 * le mock Java `ReconnaissanceMockAdapter`.
 */
export const recoMock: RecoPort = {
  async analyser(_photo, vue) {
    const vueAvecPlaque = vue == null || vue === 'avant' || vue === 'arriere';
    return {
      estVehicule: true,
      typeVehicule: 'voiture',
      plaque: vueAvecPlaque ? '0987611616' : null,
      confiance: vueAvecPlaque ? 0.92 : 0,
      confianceVehicule: 0.96,
    };
  },
};
