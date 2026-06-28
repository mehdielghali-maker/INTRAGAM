import { ResultatReco } from './types';

/**
 * Port « reconnaissance » côté front : capacité INDÉPENDANTE et REMPLAÇABLE. L'app ne
 * dépend que de cette interface ; l'implémentation (mock dev / microservice RECO réel) est
 * choisie par configuration (VITE_RECO_MODE). Ce port LIT une plaque, il ne JUGE pas la
 * conformité — la comparaison au contrat se fait dans `verification.ts`.
 */
export interface RecoPort {
  /**
   * @param photo contenu binaire de l'image (Blob)
   * @param vue   avant / arriere / gauche / droite / toit (facultatif)
   */
  analyser(photo: Blob, vue?: string | null): Promise<ResultatReco>;
}
