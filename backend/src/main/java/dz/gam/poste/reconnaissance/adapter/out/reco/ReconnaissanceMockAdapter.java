package dz.gam.poste.reconnaissance.adapter.out.reco;

import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter MOCK (dev / tests) : aucune dépendance externe.
 *
 * Actif par défaut (reco.mode=mock ou absent). Simule une lecture conforme sur les
 * vues avant/arrière et une simple détection sur les autres.
 */
@Component
@ConditionalOnProperty(name = "reco.mode", havingValue = "mock", matchIfMissing = true)
public class ReconnaissanceMockAdapter implements ReconnaissancePort {

    @Override
    public ResultatReco analyser(byte[] photo, String vue) {
        boolean vueAvecPlaque = (vue == null) || vue.equals("avant") || vue.equals("arriere");
        return new ResultatReco(
                true,
                "voiture",
                vueAvecPlaque ? "0987611616" : null,
                vueAvecPlaque ? 0.92 : 0.0,
                0.96
        );
    }
}
