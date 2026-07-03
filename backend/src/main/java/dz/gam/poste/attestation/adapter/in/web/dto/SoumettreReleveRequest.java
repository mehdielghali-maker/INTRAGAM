package dz.gam.poste.attestation.adapter.in.web.dto;

import dz.gam.poste.attestation.domain.model.LigneAttestation;
import dz.gam.poste.attestation.domain.port.in.SoumettreReleveCommand;

import java.util.List;

/**
 * Requête de POST /api/attestations/releves (contrat FIGÉ) : le mois et les lignes.
 * L'agence n'y figure pas — elle vient du contexte serveur, jamais du front. La validation
 * fine (mois AAAA-MM, lignes non vides, doublons, statuts) est portée par le DOMAINE :
 * toute violation remonte en IllegalArgumentException → HTTP 400 (handler global).
 */
public record SoumettreReleveRequest(String mois, List<LigneRequest> lignes) {

    /** Une ligne du relevé telle que confirmée/corrigée par l'AGA côté front. */
    public record LigneRequest(
            String numeroPolice,
            String numeroQuittance,
            String immatriculation,
            String assure,
            String valideDu,
            String valideAu,
            String primeTTC,
            String codeAgence,
            String statutLigne
    ) {

        LigneAttestation versDomaine() {
            return new LigneAttestation(numeroPolice, numeroQuittance, immatriculation, assure,
                    valideDu, valideAu, primeTTC, codeAgence, statutLigne);
        }
    }

    public SoumettreReleveCommand versCommande() {
        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Un relevé doit contenir au moins une ligne.");
        }
        return new SoumettreReleveCommand(mois, lignes.stream().map(LigneRequest::versDomaine).toList());
    }
}
