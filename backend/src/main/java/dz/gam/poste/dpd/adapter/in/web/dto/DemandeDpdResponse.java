package dz.gam.poste.dpd.adapter.in.web.dto;

import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.InfoClient;
import dz.gam.poste.dpd.domain.model.Souscription;
import dz.gam.poste.dpd.domain.model.StatutDpd;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vue REST d'une demande DPD (libellé de statut résolu depuis la config). */
public record DemandeDpdResponse(
        UUID id,
        String reference,
        StatutDpd statut,
        String statutLibelle,
        List<StatutDpd> prochainsStatuts,
        boolean avenant,
        String commentaire,
        Souscription souscription,
        InfoClient infoClient,
        String codeAgence,
        String mailAgence,
        String directionRegionale,
        String mailDirectionRegionale,
        String createur,
        String validateurNom,
        String validateurInitiales,
        String motifRefus,
        String codeAccord,
        Instant dateDemande,
        Instant dateMaj,
        List<PieceDto> pieces) {

    public static DemandeDpdResponse de(DemandeDpd d, Map<StatutDpd, String> libelles) {
        return new DemandeDpdResponse(
                d.id(),
                d.reference() == null ? null : d.reference().valeur(),
                d.statut(),
                libelles.getOrDefault(d.statut(), d.statut().name()),
                List.copyOf(d.statut().prochainsStatuts()),
                d.avenant(),
                d.commentaire(),
                d.souscription(),
                d.infoClient(),
                d.codeAgence(),
                d.mailAgence(),
                d.directionRegionale(),
                d.mailDirectionRegionale(),
                d.createur(),
                d.validateur() == null ? null : d.validateur().nom(),
                d.validateur() == null ? null : d.validateur().initiales(),
                d.motifRefus(),
                d.codeAccord(),
                d.dateDemande(),
                d.dateMaj(),
                d.pieces().stream().map(p -> new PieceDto(p.type(), p.nomFichier(), p.gedId())).toList());
    }
}
