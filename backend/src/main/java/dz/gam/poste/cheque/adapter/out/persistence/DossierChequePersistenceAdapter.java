package dz.gam.poste.cheque.adapter.out.persistence;

import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.Montant;
import dz.gam.poste.cheque.domain.model.ReferenceCheque;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.cheque.domain.port.out.DossierChequeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de sortie (persistance) : implémente le port {@link DossierChequeRepository}
 * en s'appuyant sur Spring Data JPA, et fait le mapping entité ↔ domaine. C'est le seul
 * endroit où l'agrégat est traduit en lignes de table et inversement.
 */
@Component
public class DossierChequePersistenceAdapter implements DossierChequeRepository {

    private final DossierChequeJpaRepository jpa;

    public DossierChequePersistenceAdapter(DossierChequeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DossierCheque enregistrer(DossierCheque dossier) {
        return versDomaine(jpa.save(versEntite(dossier)));
    }

    @Override
    public Optional<DossierCheque> trouverParId(UUID id) {
        return jpa.findById(id).map(this::versDomaine);
    }

    @Override
    public Optional<DossierCheque> trouverParReference(ReferenceCheque reference) {
        return jpa.findByReference(reference.valeur()).map(this::versDomaine);
    }

    @Override
    public List<DossierCheque> lister(FiltreDossier filtre) {
        return jpa.rechercher(filtre.statut().orElse(null), filtre.agence().orElse(null))
                .stream()
                .map(this::versDomaine)
                .toList();
    }

    private DossierChequeJpaEntity versEntite(DossierCheque d) {
        return new DossierChequeJpaEntity(
                d.id(),
                d.reference().valeur(),
                d.montant().valeur(),
                d.montant().devise(),
                d.beneficiaire(),
                d.agence(),
                d.dateEmission(),
                d.statut(),
                d.dateCreation(),
                d.dateDerniereMaj());
    }

    private DossierCheque versDomaine(DossierChequeJpaEntity e) {
        return DossierCheque.reconstituer(
                e.getId(),
                new ReferenceCheque(e.getReference()),
                new Montant(e.getMontant(), e.getDevise()),
                e.getBeneficiaire(),
                e.getAgence(),
                e.getDateEmission(),
                e.getStatut(),
                e.getDateCreation(),
                e.getDateDerniereMaj());
    }
}
