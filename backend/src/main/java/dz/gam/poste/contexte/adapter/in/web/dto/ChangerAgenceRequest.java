package dz.gam.poste.contexte.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Corps de la requête de changement d'agence active. */
public record ChangerAgenceRequest(@NotBlank String code) {
}
