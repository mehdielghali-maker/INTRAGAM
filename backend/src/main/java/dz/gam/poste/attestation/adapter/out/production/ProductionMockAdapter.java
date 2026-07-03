package dz.gam.poste.attestation.adapter.out.production;

import dz.gam.poste.attestation.domain.model.ReleveProduction;
import dz.gam.poste.attestation.domain.port.out.ProductionPort;
import org.springframework.stereotype.Component;

/**
 * Adapter MOCK du système de production (seam PROASSUR/Sage) : acquitte immédiatement la
 * soumission. À remplacer par l'adapter réel (bus / API centrale) sans toucher le domaine —
 * même contrat {@link ProductionPort}.
 */
@Component
public class ProductionMockAdapter implements ProductionPort {

    @Override
    public RetourProduction soumettre(ReleveProduction releve) {
        return new RetourProduction(releve.reference(),
                "Production validée — relevé " + releve.reference() + " transmis.");
    }
}
