package dz.gam.poste.attestation.domain.port.in;

import dz.gam.poste.attestation.domain.model.ReleveProduction;

import java.util.List;

/**
 * Cas d'usage : consulter les relevés VALIDÉS du périmètre actif (l'agence active, ou tout
 * le périmètre en vue consolidée — lecture seule).
 */
public interface ConsulterRelevesUseCase {

    List<ReleveProduction> relevesValides();
}
