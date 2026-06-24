package dz.gam.poste.dpd.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemandeDpdTest {

    private static final Instant T0 = Instant.parse("2026-06-23T10:00:00Z");

    private Souscription souscription() {
        return new Souscription("PR-88231", LocalDate.of(2026, 6, 1), "M. Saïdi", new BigDecimal("1240000"),
                "Auto — flotte", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30), 6, null);
    }

    private InfoClient client() {
        return new InfoClient("SARL Méditerranée", "SARL Méditerranée", "021", "RC123",
                TypePersonne.MORALE, false, "Alger");
    }

    private DemandeDpd brouillon(List<PieceJointeDpd> pieces) {
        return DemandeDpd.creerBrouillon(souscription(), client(), false, "DPD 6 mois",
                "16.I.Gharbi", "ag@gam.dz", "DR Alger-Est", "dr@gam.dz", "M. Saïdi", pieces, T0);
    }

    @Test
    void envoi_bloque_si_le_rc_est_absent() {
        DemandeDpd d = brouillon(List.of(new PieceJointeDpd(TypePiece.AUTRE, "photo.jpg", "GED-1")));
        assertThatThrownBy(() -> d.envoyer(new ReferenceDpd("DE-2026-0232"), T0))
                .isInstanceOf(DpdExceptions.RcManquant.class);
        assertThat(d.statut()).isEqualTo(StatutDpd.BROUILLON);
    }

    @Test
    void envoi_ok_avec_rc_emet_l_evenement() {
        DemandeDpd d = brouillon(List.of(new PieceJointeDpd(TypePiece.RC, "rc.pdf", "GED-RC-1")));
        d.envoyer(new ReferenceDpd("DE-2026-0232"), T0);

        assertThat(d.statut()).isEqualTo(StatutDpd.ENVOYEE);
        assertThat(d.reference().valeur()).isEqualTo("DE-2026-0232");
        assertThat(d.evenementsNonPublies()).hasSize(1);
        assertThat(d.validateur()).isNull(); // « non affecté » avant prise en charge
    }

    @Test
    void cycle_jusqu_a_accordee() {
        DemandeDpd d = brouillon(List.of(new PieceJointeDpd(TypePiece.RC, "rc.pdf", "GED-RC-1")));
        d.envoyer(new ReferenceDpd("DE-2026-0232"), T0);
        d.mettreEnValidation(new Validateur("DR Centre — H. Brahimi"), T0);
        assertThat(d.statut()).isEqualTo(StatutDpd.EN_VALIDATION);
        assertThat(d.validateur().initiales()).isEqualTo("DC");

        d.accorder("AC-0232", T0);
        assertThat(d.statut()).isEqualTo(StatutDpd.ACCORDEE);
        assertThat(d.codeAccord()).isEqualTo("AC-0232");
    }

    @Test
    void refus_avec_motif() {
        DemandeDpd d = brouillon(List.of(new PieceJointeDpd(TypePiece.RC, "rc.pdf", "GED-RC-1")));
        d.envoyer(new ReferenceDpd("DE-2026-0232"), T0);
        d.mettreEnValidation(new Validateur("Central — N. Lounis"), T0);
        d.refuser("Antériorité d'impayés", T0);

        assertThat(d.statut()).isEqualTo(StatutDpd.REFUSEE);
        assertThat(d.motifRefus()).isEqualTo("Antériorité d'impayés");
    }

    @Test
    void transition_interdite_refusee() {
        DemandeDpd d = brouillon(List.of(new PieceJointeDpd(TypePiece.RC, "rc.pdf", "GED-RC-1")));
        assertThatThrownBy(() -> d.accorder("AC-1", T0))
                .isInstanceOf(DpdExceptions.TransitionInvalide.class);
    }
}
