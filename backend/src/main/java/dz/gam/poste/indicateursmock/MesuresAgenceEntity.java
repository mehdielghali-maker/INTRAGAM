package dz.gam.poste.indicateursmock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Mesures éditables d'une agence (substitut du Cube Power BI). Une ligne par code agence
 * (feuille). Alimente les KPI de l'accueil. Modifiable via l'API mock ; demain, ces valeurs
 * proviendront du Cube Power BI (adapter remplaçant, même port). Montants en DA.
 */
@Entity
@Table(name = "mock_mesures_agence")
public class MesuresAgenceEntity {

    @Id
    @Column(name = "code_agence")
    String codeAgence;

    BigDecimal caYtdN;
    BigDecimal caYtdN1;
    // CA 12 derniers mois (année glissante) — dénominateur du code couleur de l'écart.
    BigDecimal caGlissant12m;
    BigDecimal caMoisN;
    BigDecimal caMoisM1;
    BigDecimal productionMois;
    BigDecimal encaisseMois;
    BigDecimal deposeMois;
    // Cumulés (YTD) — alimentent l'écart « à régulariser » de la carte KPI (cumulé, pas du mois).
    BigDecimal encaisseCumul;
    BigDecimal deposeCumul;
    BigDecimal echuNonEncaisse;
    BigDecimal echuNonEncaisseM1;
    BigDecimal encaissementsLettres;
    BigDecimal encaissementsLettresM1;
    BigDecimal sinistres12m;
    BigDecimal primes12m;
    BigDecimal sinistres12mN1;
    BigDecimal primes12mN1;
    int contratsActifs;
    int contratsActifsVariation;

    protected MesuresAgenceEntity() {
    }
}
