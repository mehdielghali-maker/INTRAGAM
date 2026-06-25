package dz.gam.poste.versement.adapter.in.web.dto;

import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.model.Versement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Vue REST d'un versement, avec libellé de statut et prochains statuts possibles. */
public record VersementResponse(
        UUID id,
        String reference,
        String codeAgence,
        String moisSituation,
        String moisLibelle,
        BigDecimal montantVerse,
        LocalDate dateVersement,
        String referenceBordereau,
        String banque,
        String commentaire,
        String createur,
        StatutVersement statut,
        String statutLibelle,
        List<StatutVersement> prochainsStatuts,
        String referenceBpm,
        String motifRejet,
        Instant dateDepot,
        Instant dateCreation,
        Instant dateMaj,
        List<PieceJustificativeDto> pieces) {

    public static VersementResponse de(Versement v) {
        return new VersementResponse(
                v.id(),
                v.reference(),
                v.codeAgence(),
                v.mois().valeur(),
                v.mois().libelle(),
                v.montantVerse(),
                v.dateVersement(),
                v.referenceBordereau(),
                v.banque(),
                v.commentaire(),
                v.createur(),
                v.statut(),
                libelle(v.statut()),
                List.copyOf(v.statut().prochainsStatuts()),
                v.referenceBpm(),
                v.motifRejet(),
                v.dateDepot(),
                v.dateCreation(),
                v.dateMaj(),
                v.pieces().stream().map(p -> new PieceJustificativeDto(p.nomFichier(), p.gedId())).toList());
    }

    private static String libelle(StatutVersement statut) {
        return switch (statut) {
            case BROUILLON -> "Brouillon";
            case DEPOSE -> "Déposé";
            case EN_CONTROLE -> "En contrôle";
            case VALIDE -> "Validé";
            case REJETE -> "Rejeté";
        };
    }
}
