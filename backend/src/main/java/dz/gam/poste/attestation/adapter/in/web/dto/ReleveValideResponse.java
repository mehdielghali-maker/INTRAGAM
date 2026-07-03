package dz.gam.poste.attestation.adapter.in.web.dto;

import dz.gam.poste.attestation.domain.model.ReleveProduction;

import java.time.Instant;

/** Élément de GET /api/attestations/releves (contrat FIGÉ) : un relevé validé du périmètre. */
public record ReleveValideResponse(String reference, String mois, String codeAgence,
                                   int nombreLignes, Instant dateValidation) {

    public static ReleveValideResponse de(ReleveProduction releve) {
        return new ReleveValideResponse(releve.reference(), releve.mois(), releve.codeAgence(),
                releve.nombreLignes(), releve.dateValidation());
    }
}
