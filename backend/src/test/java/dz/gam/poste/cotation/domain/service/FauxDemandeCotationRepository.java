package dz.gam.poste.cotation.domain.service;

import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.ReferenceDemande;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
import dz.gam.poste.cotation.domain.port.out.DemandeCotationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Double de test en mémoire du repository des demandes (aucune dépendance JPA). */
class FauxDemandeCotationRepository implements DemandeCotationRepository {

    private final Map<UUID, DemandeCotation> parId = new LinkedHashMap<>();

    @Override
    public DemandeCotation enregistrer(DemandeCotation demande) {
        parId.put(demande.id(), demande);
        return demande;
    }

    @Override
    public Optional<DemandeCotation> trouverParId(UUID id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public Optional<DemandeCotation> trouverParReference(ReferenceDemande reference) {
        return parId.values().stream()
                .filter(d -> d.reference() != null && d.reference().equals(reference))
                .findFirst();
    }

    @Override
    public List<DemandeCotation> lister(FiltreDemande filtre) {
        List<DemandeCotation> resultat = new ArrayList<>();
        for (DemandeCotation d : parId.values()) {
            if (filtre.statut().map(s -> s == d.statut()).orElse(true)) {
                resultat.add(d);
            }
        }
        return resultat;
    }

    @Override
    public long compterReferencees() {
        return parId.values().stream().filter(d -> d.reference() != null).count();
    }
}
