package dz.gam.poste.cheque.adapter.in.web.dto;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.StatutCheque;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vue REST d'un dossier de chèque. Inclut {@code prochainsStatuts} pour que l'UI sache
 * quelles transitions proposer sans réimplémenter les règles du cycle de vie.
 */
public record DossierChequeResponse(
        UUID id,
        String reference,
        BigDecimal montant,
        String devise,
        String beneficiaire,
        String agence,
        LocalDate dateEmission,
        StatutCheque statut,
        List<StatutCheque> prochainsStatuts,
        Instant dateCreation,
        Instant dateDerniereMaj) {

    public static DossierChequeResponse de(DossierCheque d) {
        return new DossierChequeResponse(
                d.id(),
                d.reference().valeur(),
                d.montant().valeur(),
                d.montant().devise(),
                d.beneficiaire(),
                d.agence(),
                d.dateEmission(),
                d.statut(),
                List.copyOf(d.statut().prochainsStatuts()),
                d.dateCreation(),
                d.dateDerniereMaj());
    }
}
