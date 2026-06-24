package dz.gam.poste.dpd.adapter.out.proassurmock;

import dz.gam.poste.dpd.domain.model.Echeance;
import dz.gam.poste.dpd.domain.model.InfoClient;
import dz.gam.poste.dpd.domain.model.ResumeAccord;
import dz.gam.poste.dpd.domain.model.Souscription;
import dz.gam.poste.dpd.domain.model.StatutReglement;
import dz.gam.poste.dpd.domain.model.TypePersonne;
import dz.gam.poste.dpd.domain.port.out.AccordProassur;
import dz.gam.poste.dpd.domain.port.out.PrefillProposition;
import dz.gam.poste.dpd.domain.port.out.ProassurDpdPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MOCK PROASSUR pour le DPD. Données fictives cohérentes avec la maquette. À remplacer par
 * l'API réelle sans toucher au domaine.
 *
 * <p>L'échéancier renvoyé porte des états de règlement DÉJÀ établis (rapprochement
 * PROASSUR × Sage en amont) — le module ne fait que les refléter.
 */
@Component
public class ProassurDpdMockAdapter implements ProassurDpdPort {

    @Override
    public Optional<PrefillProposition> prefillFromProposition(String noProposition) {
        if (noProposition == null || !noProposition.trim().toUpperCase().startsWith("PR-")) {
            return Optional.empty(); // proposition inconnue
        }
        String prop = noProposition.trim().toUpperCase();
        Souscription souscription = new Souscription(
                prop, LocalDate.of(2026, 6, 1), "M. Saïdi", new BigDecimal("1240000"),
                "Auto — flotte", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30), 6, null);
        InfoClient client = new InfoClient(
                "SARL Méditerranée Logistic", "SARL Méditerranée Logistic", "021 55 66 77",
                "16/00-1234567 B 09", TypePersonne.MORALE, false, "Zone industrielle, Rouiba, Alger");
        return Optional.of(new PrefillProposition(souscription, client));
    }

    @Override
    public Optional<AccordProassur> getAccordByCode(String codeAccord) {
        // Accepte les formats « AC-… » et « ACC-2026-… » de la maquette ; inconnu sinon.
        if (codeAccord == null || !codeAccord.trim().toUpperCase().startsWith("AC")) {
            return Optional.empty();
        }
        String code = codeAccord.trim().toUpperCase();
        ResumeAccord resume = new ResumeAccord(
                code, "SARL Méditerranée Logistic", "PR-88231",
                new BigDecimal("1240000"), "Accordée — actif", Instant.parse("2026-06-20T08:00:00Z"));
        return Optional.of(new AccordProassur(resume, echeancierType()));
    }

    /** Échéancier de la maquette : 8 × 155 000 = 1 240 000 ; 5 réglées, 1 échue, 2 à échoir. */
    private List<Echeance> echeancierType() {
        BigDecimal montant = new BigDecimal("155000");
        List<Echeance> e = new ArrayList<>();
        e.add(new Echeance(1, LocalDate.of(2026, 1, 15), montant, StatutReglement.REGLEE, LocalDate.of(2026, 1, 14)));
        e.add(new Echeance(2, LocalDate.of(2026, 2, 15), montant, StatutReglement.REGLEE, LocalDate.of(2026, 2, 13)));
        e.add(new Echeance(3, LocalDate.of(2026, 3, 15), montant, StatutReglement.REGLEE, LocalDate.of(2026, 3, 17)));
        e.add(new Echeance(4, LocalDate.of(2026, 4, 15), montant, StatutReglement.REGLEE, LocalDate.of(2026, 4, 15)));
        e.add(new Echeance(5, LocalDate.of(2026, 5, 15), montant, StatutReglement.REGLEE, LocalDate.of(2026, 5, 19)));
        e.add(new Echeance(6, LocalDate.of(2026, 6, 15), montant, StatutReglement.ECHUE, null));
        e.add(new Echeance(7, LocalDate.of(2026, 7, 15), montant, StatutReglement.A_ECHOIR, null));
        e.add(new Echeance(8, LocalDate.of(2026, 8, 15), montant, StatutReglement.A_ECHOIR, null));
        return e;
    }
}
