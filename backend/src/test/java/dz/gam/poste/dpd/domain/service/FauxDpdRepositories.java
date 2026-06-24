package dz.gam.poste.dpd.domain.service;

import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.ReferenceDpd;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.dpd.domain.port.out.AccordSuiviRepository;
import dz.gam.poste.dpd.domain.port.out.DemandeDpdRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doubles de test en mémoire des repositories DPD. */
final class FauxDpdRepositories {

    private FauxDpdRepositories() {
    }

    static class Demandes implements DemandeDpdRepository {
        private final Map<UUID, DemandeDpd> parId = new LinkedHashMap<>();

        @Override
        public DemandeDpd enregistrer(DemandeDpd d) {
            parId.put(d.id(), d);
            return d;
        }

        @Override
        public Optional<DemandeDpd> trouverParId(UUID id) {
            return Optional.ofNullable(parId.get(id));
        }

        @Override
        public Optional<DemandeDpd> trouverParReference(ReferenceDpd reference) {
            return parId.values().stream()
                    .filter(d -> d.reference() != null && d.reference().equals(reference))
                    .findFirst();
        }

        @Override
        public List<DemandeDpd> lister(FiltreDpd filtre) {
            List<DemandeDpd> r = new ArrayList<>();
            for (DemandeDpd d : parId.values()) {
                if (filtre.statut().map(s -> s == d.statut()).orElse(true)) {
                    r.add(d);
                }
            }
            return r;
        }

        @Override
        public long compterReferencees() {
            return parId.values().stream().filter(d -> d.reference() != null).count();
        }
    }

    static class Accords implements AccordSuiviRepository {
        private final Map<String, AccordSuivi> parCode = new LinkedHashMap<>();

        @Override
        public AccordSuivi enregistrer(AccordSuivi a) {
            parCode.put(a.codeAccord(), a);
            return a;
        }

        @Override
        public Optional<AccordSuivi> trouverParCodeAccord(String codeAccord) {
            return Optional.ofNullable(parCode.get(codeAccord));
        }
    }
}
