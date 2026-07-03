package dz.gam.poste.attestation.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dz.gam.poste.attestation.domain.model.LigneAttestation;
import dz.gam.poste.attestation.domain.model.ReleveProduction;
import dz.gam.poste.attestation.domain.model.StatutReleve;
import dz.gam.poste.attestation.domain.port.out.ReleveProductionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistance des relevés de production. Lignes stockées en JSON (pattern DPD). */
@Component
public class ReleveProductionPersistenceAdapter implements ReleveProductionRepository {

    private final ReleveProductionJpaRepository jpa;
    private final ObjectMapper mapper;

    public ReleveProductionPersistenceAdapter(ReleveProductionJpaRepository jpa, ObjectMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    /** Charge utile JSON : le statut et les lignes du lot (champs non indexés). */
    private record Payload(StatutReleve statut, List<LigneAttestation> lignes) {
    }

    @Override
    public ReleveProduction enregistrer(ReleveProduction releve) {
        ReleveProductionJpaEntity e = jpa.findByReference(releve.reference())
                .orElseGet(() -> {
                    ReleveProductionJpaEntity neuf = new ReleveProductionJpaEntity();
                    neuf.setId(UUID.randomUUID());
                    return neuf;
                });
        e.setReference(releve.reference());
        e.setCodeAgence(releve.codeAgence());
        e.setMois(releve.mois());
        e.setNombreLignes(releve.nombreLignes());
        e.setDateValidation(releve.dateValidation());
        e.setPayloadJson(ecrire(new Payload(releve.statut(), releve.lignes())));
        return versDomaine(jpa.save(e));
    }

    @Override
    public Optional<ReleveProduction> trouverParAgenceEtMois(String codeAgence, String mois) {
        return jpa.findByCodeAgenceAndMois(codeAgence, mois).map(this::versDomaine);
    }

    @Override
    public List<ReleveProduction> listerPourAgences(List<String> codesAgences) {
        if (codesAgences.isEmpty()) {
            return List.of(); // périmètre vide : rien à lister (et IN () serait invalide)
        }
        return jpa.findByCodeAgenceInOrderByDateValidationDesc(codesAgences)
                .stream().map(this::versDomaine).toList();
    }

    private ReleveProduction versDomaine(ReleveProductionJpaEntity e) {
        Payload p = lire(e.getPayloadJson());
        return new ReleveProduction(e.getReference(), e.getMois(), e.getCodeAgence(),
                p.lignes(), p.statut(), e.getDateValidation());
    }

    private String ecrire(Payload payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Sérialisation du payload attestation impossible", ex);
        }
    }

    private Payload lire(String json) {
        try {
            return mapper.readValue(json, Payload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Lecture du payload attestation impossible", ex);
        }
    }
}
