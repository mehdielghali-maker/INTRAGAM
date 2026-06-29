package dz.gam.poste.versement.adapter.out.persistence;

import dz.gam.poste.versement.adapter.out.persistence.VersementJpaEntity.PieceEmbeddable;
import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.model.PieceJustificative;
import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistance : mapping agrégat ↔ entité JPA. */
@Component
public class VersementPersistenceAdapter implements VersementRepository {

    private final VersementJpaRepository jpa;

    public VersementPersistenceAdapter(VersementJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Versement enregistrer(Versement versement) {
        return versDomaine(jpa.save(versEntite(versement)));
    }

    @Override
    public Optional<Versement> trouverParId(UUID id) {
        return jpa.findById(id).map(this::versDomaine);
    }

    @Override
    public Optional<Versement> trouverParReference(String reference) {
        return jpa.findByReference(reference).map(this::versDomaine);
    }

    @Override
    public boolean existsByCodeAgence(String codeAgence) {
        return jpa.existsByCodeAgence(codeAgence);
    }

    @Override
    public List<Versement> lister(FiltreVersement filtre) {
        return jpa.rechercher(filtre.codeAgence(), filtre.statut()).stream().map(this::versDomaine).toList();
    }

    @Override
    public long compterEnCours() {
        return jpa.countByStatutIn(List.of(StatutVersement.DEPOSE, StatutVersement.EN_CONTROLE));
    }

    @Override
    public long compterReferences() {
        return jpa.countByReferenceIsNotNull();
    }

    private VersementJpaEntity versEntite(Versement v) {
        VersementJpaEntity e = jpa.findById(v.id()).orElseGet(VersementJpaEntity::new);
        e.setId(v.id());
        e.setReference(v.reference());
        e.setCodeAgence(v.codeAgence());
        e.setMoisSituation(v.mois().valeur());
        e.setMontantVerse(v.montantVerse());
        e.setDateVersement(v.dateVersement());
        e.setReferenceBordereau(v.referenceBordereau());
        e.setBanque(v.banque());
        e.setCommentaire(v.commentaire());
        e.setCreateur(v.createur());
        e.setStatut(v.statut());
        e.setReferenceBpm(v.referenceBpm());
        e.setMotifRejet(v.motifRejet());
        e.setDateDepot(v.dateDepot());
        e.setDateCreation(v.dateCreation());
        e.setDateMaj(v.dateMaj());
        e.getPieces().clear();
        v.pieces().forEach(p -> e.getPieces().add(new PieceEmbeddable(p.nomFichier(), p.gedId())));
        return e;
    }

    private Versement versDomaine(VersementJpaEntity e) {
        List<PieceJustificative> pieces = e.getPieces().stream()
                .map(p -> new PieceJustificative(p.getNomFichier(), p.getGedId()))
                .toList();
        return Versement.reconstituer(
                e.getId(), e.getReference(), e.getCodeAgence(), MoisSituation.depuis(e.getMoisSituation()),
                e.getMontantVerse(), e.getDateVersement(), e.getReferenceBordereau(), e.getBanque(),
                e.getCommentaire(), e.getCreateur(), pieces, e.getStatut(), e.getReferenceBpm(),
                e.getMotifRejet(), e.getDateDepot(), e.getDateCreation(), e.getDateMaj());
    }
}
