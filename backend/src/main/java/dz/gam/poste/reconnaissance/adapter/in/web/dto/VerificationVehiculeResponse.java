package dz.gam.poste.reconnaissance.adapter.in.web.dto;

import dz.gam.poste.reconnaissance.domain.model.ResultatVerificationVehicule;
import dz.gam.poste.reconnaissance.domain.model.StatutVerification;

/**
 * Vue REST d'une vérification de véhicule. {@code bloquant} permet à l'UI de refuser
 * la validation (anti-fraude) sans réimplémenter la règle côté front.
 */
public record VerificationVehiculeResponse(
        StatutVerification statut,
        String plaqueLue,
        String typeVehicule,
        double confiance,
        boolean estVehicule,
        boolean bloquant) {

    public static VerificationVehiculeResponse de(ResultatVerificationVehicule r) {
        return new VerificationVehiculeResponse(
                r.statut(), r.plaqueLue(), r.typeVehicule(), r.confiance(), r.estVehicule(), r.bloquant());
    }
}
