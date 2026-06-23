package dz.gam.poste.cheque.domain.service;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.ReferenceCheque;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.cheque.domain.port.out.DossierChequeRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Double de test en mémoire du repository (aucune dépendance JPA/DB). */
class FauxDossierChequeRepository implements DossierChequeRepository {

    private final Map<UUID, DossierCheque> parId = new LinkedHashMap<>();

    @Override
    public DossierCheque enregistrer(DossierCheque dossier) {
        parId.put(dossier.id(), dossier);
        return dossier;
    }

    @Override
    public Optional<DossierCheque> trouverParId(UUID id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public Optional<DossierCheque> trouverParReference(ReferenceCheque reference) {
        return parId.values().stream()
                .filter(d -> d.reference().equals(reference))
                .findFirst();
    }

    @Override
    public List<DossierCheque> lister(FiltreDossier filtre) {
        List<DossierCheque> resultat = new ArrayList<>();
        for (DossierCheque d : parId.values()) {
            boolean statutOk = filtre.statut().map(s -> s == d.statut()).orElse(true);
            boolean agenceOk = filtre.agence().map(a -> a.equals(d.agence())).orElse(true);
            if (statutOk && agenceOk) {
                resultat.add(d);
            }
        }
        return resultat;
    }
}
