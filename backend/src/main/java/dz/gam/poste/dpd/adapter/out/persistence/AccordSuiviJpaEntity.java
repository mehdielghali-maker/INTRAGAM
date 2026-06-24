package dz.gam.poste.dpd.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** État persisté du suivi versionné d'un accord. Résumé + versions sérialisés en JSON. */
@Entity
@Table(name = "accord_suivi")
public class AccordSuiviJpaEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String codeAccord;

    @Column(nullable = false)
    private Instant dateMaj;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    protected AccordSuiviJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCodeAccord() {
        return codeAccord;
    }

    public void setCodeAccord(String codeAccord) {
        this.codeAccord = codeAccord;
    }

    public Instant getDateMaj() {
        return dateMaj;
    }

    public void setDateMaj(Instant dateMaj) {
        this.dateMaj = dateMaj;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
