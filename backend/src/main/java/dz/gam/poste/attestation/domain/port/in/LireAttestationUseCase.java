package dz.gam.poste.attestation.domain.port.in;

import dz.gam.poste.attestation.domain.model.ResultatAttestation;

/**
 * Cas d'usage : lire une attestation photographiée (OCR) pour pré-remplir la ligne du
 * relevé. La lecture est une AIDE : l'AGA confirme ou corrige toujours avant l'ajout.
 */
public interface LireAttestationUseCase {

    ResultatAttestation lire(byte[] photo);
}
