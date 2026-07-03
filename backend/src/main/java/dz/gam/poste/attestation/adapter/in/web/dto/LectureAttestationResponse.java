package dz.gam.poste.attestation.adapter.in.web.dto;

import dz.gam.poste.attestation.domain.model.ResultatAttestation;

/**
 * Réponse de POST /api/attestations/lire — contrat JSON /lire-attestation FIGÉ, partagé
 * par les trois chantiers. Les noms et formats de champs ne doivent PAS changer.
 */
public record LectureAttestationResponse(
        String numeroPolice,
        String numeroQuittance,
        String immatriculation,
        String assure,
        String valideDu,
        String valideAu,
        String primeTTC,
        String codeAgence,
        double confiance,
        String statut,
        String texteBrut
) {

    public static LectureAttestationResponse de(ResultatAttestation r) {
        return new LectureAttestationResponse(r.numeroPolice(), r.numeroQuittance(),
                r.immatriculation(), r.assure(), r.valideDu(), r.valideAu(), r.primeTTC(),
                r.codeAgence(), r.confiance(), r.statut(), r.texteBrut());
    }
}
