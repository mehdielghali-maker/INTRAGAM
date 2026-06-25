package dz.gam.poste.versement.adapter.in.web.dto;

import dz.gam.poste.versement.domain.port.in.DeposerVersementCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Corps de requête de dépôt. Le code agence et le créateur ne sont PAS acceptés ici : ils
 * viennent du contexte de session côté serveur. Le caractère obligatoire du montant et du
 * reçu est vérifié par le domaine à la soumission.
 */
public record DeposerVersementRequest(
        @NotBlank(message = "Le mois de la situation financière est obligatoire") String moisSituation,
        BigDecimal montantVerse,
        @NotNull(message = "La date du versement est obligatoire") LocalDate dateVersement,
        String referenceBordereau,
        String banque,
        String commentaire,
        List<String> nomsFichiers) {

    public DeposerVersementCommand versCommande() {
        return new DeposerVersementCommand(moisSituation, montantVerse, dateVersement,
                referenceBordereau, banque, commentaire, nomsFichiers);
    }
}
