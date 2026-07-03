package dz.gam.poste.attestation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReleveProductionTest {

    private static final Instant QUAND = Instant.parse("2026-07-03T10:00:00Z");
    private static final String AGENCE = "16.AL.0316";

    private static LigneAttestation ligne(String police) {
        return new LigneAttestation(police, "26004181", "00123-316-16", "BENALI Karim",
                "01/01/2026", "31/12/2026", "24800,50", AGENCE, LigneAttestation.STATUT_CONFIRME);
    }

    @Test
    void valider_fige_le_lot_avec_reference_deterministe_statut_et_date() {
        ReleveProduction releve = ReleveProduction.valider("2026-06", AGENCE,
                List.of(ligne("160312202600418"), ligne("160312202600505")), QUAND);

        assertThat(releve.reference()).isEqualTo("PROD-2026-06-" + AGENCE);
        assertThat(releve.statut()).isEqualTo(StatutReleve.VALIDEE);
        assertThat(releve.dateValidation()).isEqualTo(QUAND);
        assertThat(releve.nombreLignes()).isEqualTo(2);
    }

    @Test
    void mois_invalide_est_rejete() {
        for (String moisInvalide : new String[]{null, "2026-13", "2026-00", "06/2026", "2026-6", "202606"}) {
            assertThatThrownBy(() -> ReleveProduction.valider(moisInvalide, AGENCE,
                    List.of(ligne("160312202600418")), QUAND))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AAAA-MM");
        }
    }

    @Test
    void releve_sans_ligne_est_rejete() {
        assertThatThrownBy(() -> ReleveProduction.valider("2026-06", AGENCE, List.of(), QUAND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("au moins une ligne");
        assertThatThrownBy(() -> ReleveProduction.valider("2026-06", AGENCE, null, QUAND))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doublon_de_numero_de_police_est_rejete() {
        assertThatThrownBy(() -> ReleveProduction.valider("2026-06", AGENCE,
                List.of(ligne("160312202600418"), ligne("160312202600418")), QUAND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Doublon")
                .hasMessageContaining("160312202600418");
    }

    @Test
    void releve_valide_est_verrouille_les_lignes_sont_immuables() {
        ReleveProduction releve = ReleveProduction.valider("2026-06", AGENCE,
                List.of(ligne("160312202600418")), QUAND);

        // Après validation → verrouillé : impossible d'ajouter une ligne au lot figé.
        assertThatThrownBy(() -> releve.lignes().add(ligne("160312202600505")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void ligne_sans_numero_de_police_est_rejetee() {
        assertThatThrownBy(() -> new LigneAttestation(null, null, null, null, null, null, null,
                null, LigneAttestation.STATUT_CONFIRME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("police");
    }

    @Test
    void statut_de_ligne_invalide_est_rejete() {
        assertThatThrownBy(() -> new LigneAttestation("160312202600418", null, null, null, null,
                null, null, null, "valide"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Statut de ligne invalide");
    }
}
