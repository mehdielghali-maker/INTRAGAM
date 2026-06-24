package dz.gam.poste.dpd.adapter.out.persistence;

import dz.gam.poste.dpd.domain.model.StatutDpd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * État persisté d'une demande DPD. Les colonnes ne portent que les clés de requête
 * (référence, statut, dates, code accord) ; les value objects imbriqués (souscription,
 * client, pièces) sont sérialisés en JSON dans {@code payloadJson} par l'adapter.
 */
@Entity
@Table(name = "demande_dpd")
public class DemandeDpdJpaEntity {

    @Id
    private UUID id;

    @Column(unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private StatutDpd statut;

    private String codeAccord;

    @Column(nullable = false)
    private Instant dateDemande;

    @Column(nullable = false)
    private Instant dateMaj;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    protected DemandeDpdJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public StatutDpd getStatut() {
        return statut;
    }

    public void setStatut(StatutDpd statut) {
        this.statut = statut;
    }

    public String getCodeAccord() {
        return codeAccord;
    }

    public void setCodeAccord(String codeAccord) {
        this.codeAccord = codeAccord;
    }

    public Instant getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(Instant dateDemande) {
        this.dateDemande = dateDemande;
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
