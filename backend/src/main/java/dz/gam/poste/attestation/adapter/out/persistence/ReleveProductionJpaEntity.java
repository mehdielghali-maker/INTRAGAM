package dz.gam.poste.attestation.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * État persisté d'un relevé de production validé. Les colonnes ne portent que les clés de
 * requête (référence, agence, mois, date) ; les lignes du relevé sont sérialisées en JSON
 * dans {@code payloadJson} par l'adapter (pattern DPD).
 */
@Entity
@Table(name = "attestation_releve")
public class ReleveProductionJpaEntity {

    @Id
    private UUID id;

    /** Référence déterministe PROD-{mois}-{codeAgence} : unique ⇒ un lot par (agence, mois). */
    @Column(unique = true, nullable = false)
    private String reference;

    @Column(nullable = false)
    private String codeAgence;

    @Column(nullable = false, length = 7)
    private String mois;

    @Column(nullable = false)
    private int nombreLignes;

    @Column(nullable = false)
    private Instant dateValidation;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    protected ReleveProductionJpaEntity() {
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

    public String getCodeAgence() {
        return codeAgence;
    }

    public void setCodeAgence(String codeAgence) {
        this.codeAgence = codeAgence;
    }

    public String getMois() {
        return mois;
    }

    public void setMois(String mois) {
        this.mois = mois;
    }

    public int getNombreLignes() {
        return nombreLignes;
    }

    public void setNombreLignes(int nombreLignes) {
        this.nombreLignes = nombreLignes;
    }

    public Instant getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(Instant dateValidation) {
        this.dateValidation = dateValidation;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
