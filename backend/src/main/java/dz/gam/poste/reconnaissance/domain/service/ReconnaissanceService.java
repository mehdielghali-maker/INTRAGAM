package dz.gam.poste.reconnaissance.domain.service;

import dz.gam.poste.reconnaissance.domain.model.ResultatVerificationVehicule;
import dz.gam.poste.reconnaissance.domain.model.StatutVerification;
import dz.gam.poste.reconnaissance.domain.port.in.VerifierVehiculeUseCase;
import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort;

/**
 * Service applicatif de la reconnaissance véhicule : orchestre le port ANPR (lecture
 * de plaque) puis la {@link VerificationPlaqueService} (comparaison au contrat). Java
 * pur ; câblé dans la couche config. L'appel vient du poste AGA, donc {@code estAga = true}
 * pour la décision de blocage anti-fraude.
 */
public class ReconnaissanceService implements VerifierVehiculeUseCase {

    private final ReconnaissancePort reconnaissance;
    private final VerificationPlaqueService verification;
    private final boolean bloqueNonConforme;

    public ReconnaissanceService(ReconnaissancePort reconnaissance,
                                 VerificationPlaqueService verification,
                                 boolean bloqueNonConforme) {
        this.reconnaissance = reconnaissance;
        this.verification = verification;
        this.bloqueNonConforme = bloqueNonConforme;
    }

    @Override
    public ResultatVerificationVehicule verifier(byte[] photo, String vue, String immatriculation) {
        ReconnaissancePort.ResultatReco reco = reconnaissance.analyser(photo, vue);
        StatutVerification statut = verification.evaluer(reco, immatriculation, vue);
        boolean bloquant = verification.bloqueValidation(statut, true, bloqueNonConforme);
        return new ResultatVerificationVehicule(
                statut,
                reco == null ? null : reco.plaque(),
                reco == null ? null : reco.typeVehicule(),
                reco == null ? 0.0 : reco.confiance(),
                reco != null && reco.estVehicule(),
                bloquant);
    }
}
