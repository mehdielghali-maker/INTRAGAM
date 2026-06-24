package dz.gam.poste.dpd.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.EcheancierVersion;
import dz.gam.poste.dpd.domain.model.ResumeAccord;
import dz.gam.poste.dpd.domain.port.out.AccordSuiviRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adapter de persistance du suivi versionné des accords (résumé + versions en JSON). */
@Component
public class AccordSuiviPersistenceAdapter implements AccordSuiviRepository {

    private final AccordSuiviJpaRepository jpa;
    private final ObjectMapper mapper;

    public AccordSuiviPersistenceAdapter(AccordSuiviJpaRepository jpa, ObjectMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    private record Payload(ResumeAccord resume, List<EcheancierVersion> versions) {
    }

    @Override
    public AccordSuivi enregistrer(AccordSuivi a) {
        AccordSuiviJpaEntity e = jpa.findById(a.id()).orElseGet(AccordSuiviJpaEntity::new);
        e.setId(a.id());
        e.setCodeAccord(a.codeAccord());
        e.setDateMaj(a.dateMaj());
        e.setPayloadJson(ecrire(new Payload(a.resume(), a.versions())));
        return versDomaine(jpa.save(e));
    }

    @Override
    public Optional<AccordSuivi> trouverParCodeAccord(String codeAccord) {
        return jpa.findByCodeAccord(codeAccord).map(this::versDomaine);
    }

    private AccordSuivi versDomaine(AccordSuiviJpaEntity e) {
        Payload p = lire(e.getPayloadJson());
        return AccordSuivi.reconstituer(e.getId(), e.getCodeAccord(), p.resume(), p.versions(), e.getDateMaj());
    }

    private String ecrire(Payload payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Sérialisation du suivi d'accord impossible", ex);
        }
    }

    private Payload lire(String json) {
        try {
            return mapper.readValue(json, Payload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Lecture du suivi d'accord impossible", ex);
        }
    }
}
