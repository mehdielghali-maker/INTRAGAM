package dz.gam.poste.indicateursmock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Situation financière éditable d'une agence pour un mois (substitut du Cube Power BI).
 * Alimente la fonction « Versement bancaire » (production émise / encaissé / versé en banque).
 * Montants en DA.
 */
@Entity
@Table(name = "mock_situation_mensuelle",
        uniqueConstraints = @UniqueConstraint(columnNames = {"code_agence", "mois"}))
public class SituationMensuelleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "code_agence", nullable = false)
    String codeAgence;

    @Column(nullable = false, length = 7)
    String mois;            // AAAA-MM

    BigDecimal productionEmise;
    BigDecimal encaisse;
    BigDecimal verse;

    protected SituationMensuelleEntity() {
    }
}
