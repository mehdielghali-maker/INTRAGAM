package dz.gam.poste.dpd.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.InfoClient;
import dz.gam.poste.dpd.domain.model.PieceJointeDpd;
import dz.gam.poste.dpd.domain.model.ReferenceDpd;
import dz.gam.poste.dpd.domain.model.Souscription;
import dz.gam.poste.dpd.domain.model.Validateur;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.dpd.domain.port.out.DemandeDpdRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistance des demandes DPD. Value objects imbriqués stockés en JSON. */
@Component
public class DemandeDpdPersistenceAdapter implements DemandeDpdRepository {

    private final DemandeDpdJpaRepository jpa;
    private final ObjectMapper mapper;

    public DemandeDpdPersistenceAdapter(DemandeDpdJpaRepository jpa, ObjectMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    /** Charge utile JSON regroupant les value objects et champs non indexés. */
    private record Payload(Souscription souscription, InfoClient infoClient, boolean avenant, String commentaire,
                           String codeAgence, String mailAgence, String directionRegionale,
                           String mailDirectionRegionale, String createur, List<PieceJointeDpd> pieces,
                           String validateurNom, String motifRefus) {
    }

    @Override
    public DemandeDpd enregistrer(DemandeDpd d) {
        DemandeDpdJpaEntity e = jpa.findById(d.id()).orElseGet(DemandeDpdJpaEntity::new);
        e.setId(d.id());
        e.setReference(d.reference() == null ? null : d.reference().valeur());
        e.setStatut(d.statut());
        e.setCodeAccord(d.codeAccord());
        e.setDateDemande(d.dateDemande());
        e.setDateMaj(d.dateMaj());
        e.setPayloadJson(ecrire(new Payload(
                d.souscription(), d.infoClient(), d.avenant(), d.commentaire(),
                d.codeAgence(), d.mailAgence(), d.directionRegionale(), d.mailDirectionRegionale(),
                d.createur(), d.pieces(),
                d.validateur() == null ? null : d.validateur().nom(), d.motifRefus())));
        return versDomaine(jpa.save(e));
    }

    @Override
    public Optional<DemandeDpd> trouverParId(UUID id) {
        return jpa.findById(id).map(this::versDomaine);
    }

    @Override
    public Optional<DemandeDpd> trouverParReference(ReferenceDpd reference) {
        return jpa.findByReference(reference.valeur()).map(this::versDomaine);
    }

    @Override
    public List<DemandeDpd> lister(FiltreDpd filtre) {
        return jpa.rechercher(filtre.statut().orElse(null)).stream().map(this::versDomaine).toList();
    }

    @Override
    public long compterReferencees() {
        return jpa.countByReferenceIsNotNull();
    }

    private DemandeDpd versDomaine(DemandeDpdJpaEntity e) {
        Payload p = lire(e.getPayloadJson());
        return DemandeDpd.reconstituer(
                e.getId(),
                e.getReference() == null ? null : new ReferenceDpd(e.getReference()),
                e.getStatut(), p.souscription(), p.infoClient(), p.avenant(), p.commentaire(),
                p.codeAgence(), p.mailAgence(), p.directionRegionale(), p.mailDirectionRegionale(), p.createur(),
                p.pieces(),
                p.validateurNom() == null ? null : new Validateur(p.validateurNom()),
                p.motifRefus(), e.getCodeAccord(), e.getDateDemande(), e.getDateMaj());
    }

    private String ecrire(Payload payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Sérialisation du payload DPD impossible", ex);
        }
    }

    private Payload lire(String json) {
        try {
            return mapper.readValue(json, Payload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Lecture du payload DPD impossible", ex);
        }
    }
}
