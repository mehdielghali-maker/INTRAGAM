package dz.gam.poste.cotation.adapter.in.web.dto;

import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.StatutCotation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vue REST d'une demande de cotation. Inclut le libellé de statut résolu depuis la config. */
public record DemandeResponse(
        UUID id,
        String reference,
        String objet,
        String nomProspect,
        String numeroPolice,
        boolean renouvellement,
        String commentaire,
        String codeAgence,
        String directionRegionale,
        String createur,
        StatutCotation statut,
        String statutLibelle,
        List<StatutCotation> prochainsStatuts,
        String souscripteurNom,
        String souscripteurInitiales,
        String numeroProposition,
        String referenceDevis,
        String motifSansSuite,
        Instant dateQuittance,
        Instant dateCreation,
        Instant dateMaj,
        List<PieceJointeDto> piecesJointes) {

    public static DemandeResponse de(DemandeCotation d, Map<StatutCotation, String> libelles) {
        return new DemandeResponse(
                d.id(),
                d.reference() == null ? null : d.reference().valeur(),
                d.objet(),
                d.nomProspect(),
                d.numeroPolice(),
                d.renouvellement(),
                d.commentaire(),
                d.codeAgence(),
                d.directionRegionale(),
                d.createur(),
                d.statut(),
                libelles.getOrDefault(d.statut(), d.statut().name()),
                List.copyOf(d.statut().prochainsStatuts()),
                d.souscripteur() == null ? null : d.souscripteur().nom(),
                d.souscripteur() == null ? null : d.souscripteur().initiales(),
                d.numeroProposition(),
                d.referenceDevis(),
                d.motifSansSuite(),
                d.dateQuittance(),
                d.dateCreation(),
                d.dateMaj(),
                d.piecesJointes().stream().map(p -> new PieceJointeDto(p.nomFichier(), p.gedId())).toList());
    }
}
