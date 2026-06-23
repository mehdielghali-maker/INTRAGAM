package dz.gam.poste.cheque.adapter.in.web.dto;

import dz.gam.poste.cheque.domain.model.StatutCheque;
import jakarta.validation.constraints.NotNull;

/** Corps de requête pour faire avancer le statut d'un dossier. */
public record FaireAvancerStatutRequest(
        @NotNull(message = "statutCible est obligatoire") StatutCheque statutCible) {
}
