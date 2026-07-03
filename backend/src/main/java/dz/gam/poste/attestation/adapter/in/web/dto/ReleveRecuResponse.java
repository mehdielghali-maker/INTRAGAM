package dz.gam.poste.attestation.adapter.in.web.dto;

import dz.gam.poste.attestation.domain.port.in.SoumettreReleveUseCase;

/** Réponse de POST /api/attestations/releves (contrat FIGÉ) : l'accusé de soumission. */
public record ReleveRecuResponse(String reference, String mois, String codeAgence,
                                 int nombreLignes, String message) {

    public static ReleveRecuResponse de(SoumettreReleveUseCase.RecuReleve recu) {
        return new ReleveRecuResponse(recu.reference(), recu.mois(), recu.codeAgence(),
                recu.nombreLignes(), recu.message());
    }
}
