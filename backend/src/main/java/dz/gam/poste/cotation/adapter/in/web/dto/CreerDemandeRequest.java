package dz.gam.poste.cotation.adapter.in.web.dto;

import dz.gam.poste.cotation.domain.port.in.CreerDemandeCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Corps de requête de création/envoi. Le code agence, la DR et le créateur ne sont PAS
 * acceptés ici : ils viennent de l'identité SSO côté serveur.
 */
public record CreerDemandeRequest(
        @NotBlank(message = "La branche / objet est obligatoire") String objet,
        @NotBlank(message = "Le nom du prospect est obligatoire") String nomProspect,
        String numeroPolice,
        String commentaire,
        List<String> nomsFichiers) {

    public CreerDemandeCommand versCommande() {
        return new CreerDemandeCommand(objet, nomProspect, numeroPolice, commentaire, nomsFichiers);
    }
}
