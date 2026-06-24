package dz.gam.poste.dpd.adapter.in.web.dto;

import dz.gam.poste.dpd.domain.model.TypePersonne;
import dz.gam.poste.dpd.domain.port.in.CreerDpdCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Corps de requête de création/envoi d'une demande DPD. */
public record CreerDpdRequest(
        @NotBlank(message = "Le n° de proposition est obligatoire") String noProposition,
        boolean avenant,
        String commentaire,
        @NotBlank(message = "Le nom de l'assuré est obligatoire") String nomAssure,
        @NotBlank(message = "Le nom du souscripteur est obligatoire") String nomSouscripteur,
        String telephone,
        String cnrc,
        TypePersonne typePersonne,
        boolean institutionPublique,
        String adresse,
        List<String> fichiersRc,
        List<String> fichiersAutres) {

    public CreerDpdCommand versCommande() {
        return new CreerDpdCommand(noProposition, avenant, commentaire, nomAssure, nomSouscripteur,
                telephone, cnrc, typePersonne == null ? TypePersonne.MORALE : typePersonne,
                institutionPublique, adresse, fichiersRc, fichiersAutres);
    }
}
