package dz.gam.poste.cotation.adapter.out.persistence;

import dz.gam.poste.cotation.adapter.out.persistence.DemandeCotationJpaEntity.PieceJointeEmbeddable;
import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.PieceJointe;
import dz.gam.poste.cotation.domain.model.ReferenceDemande;
import dz.gam.poste.cotation.domain.model.Souscripteur;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
import dz.gam.poste.cotation.domain.port.out.DemandeCotationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistance : mapping agrégat ↔ entité JPA. */
@Component
public class DemandeCotationPersistenceAdapter implements DemandeCotationRepository {

    private final DemandeCotationJpaRepository jpa;

    public DemandeCotationPersistenceAdapter(DemandeCotationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DemandeCotation enregistrer(DemandeCotation demande) {
        return versDomaine(jpa.save(versEntite(demande)));
    }

    @Override
    public Optional<DemandeCotation> trouverParId(UUID id) {
        return jpa.findById(id).map(this::versDomaine);
    }

    @Override
    public Optional<DemandeCotation> trouverParReference(ReferenceDemande reference) {
        return jpa.findByReference(reference.valeur()).map(this::versDomaine);
    }

    @Override
    public List<DemandeCotation> lister(FiltreDemande filtre) {
        return jpa.rechercher(filtre.statut().orElse(null)).stream().map(this::versDomaine).toList();
    }

    @Override
    public long compterReferencees() {
        return jpa.countByReferenceIsNotNull();
    }

    private DemandeCotationJpaEntity versEntite(DemandeCotation d) {
        DemandeCotationJpaEntity e = jpa.findById(d.id()).orElseGet(DemandeCotationJpaEntity::new);
        e.setId(d.id());
        e.setReference(d.reference() == null ? null : d.reference().valeur());
        e.setObjet(d.objet());
        e.setNomProspect(d.nomProspect());
        e.setNumeroPolice(d.numeroPolice());
        e.setRenouvellement(d.renouvellement());
        e.setCommentaire(d.commentaire());
        e.setCodeAgence(d.codeAgence());
        e.setDirectionRegionale(d.directionRegionale());
        e.setCreateur(d.createur());
        e.setStatut(d.statut());
        e.setSouscripteurNom(d.souscripteur() == null ? null : d.souscripteur().nom());
        e.setNumeroProposition(d.numeroProposition());
        e.setReferenceDevis(d.referenceDevis());
        e.setMotifSansSuite(d.motifSansSuite());
        e.setDateQuittance(d.dateQuittance());
        e.setDateCreation(d.dateCreation());
        e.setDateMaj(d.dateMaj());
        e.getPiecesJointes().clear();
        d.piecesJointes().forEach(p -> e.getPiecesJointes()
                .add(new PieceJointeEmbeddable(p.nomFichier(), p.gedId())));
        return e;
    }

    private DemandeCotation versDomaine(DemandeCotationJpaEntity e) {
        List<PieceJointe> pieces = e.getPiecesJointes().stream()
                .map(p -> new PieceJointe(p.getNomFichier(), p.getGedId()))
                .toList();
        return DemandeCotation.reconstituer(
                e.getId(),
                e.getReference() == null ? null : new ReferenceDemande(e.getReference()),
                e.getObjet(), e.getNomProspect(), e.getNumeroPolice(), e.isRenouvellement(),
                e.getCommentaire(), e.getCodeAgence(), e.getDirectionRegionale(), e.getCreateur(),
                pieces, e.getStatut(),
                e.getSouscripteurNom() == null ? null : new Souscripteur(e.getSouscripteurNom()),
                e.getNumeroProposition(), e.getReferenceDevis(), e.getMotifSansSuite(),
                e.getDateQuittance(), e.getDateCreation(), e.getDateMaj());
    }
}
