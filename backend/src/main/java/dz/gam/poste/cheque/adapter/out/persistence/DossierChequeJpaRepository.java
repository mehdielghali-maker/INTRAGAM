package dz.gam.poste.cheque.adapter.out.persistence;

import dz.gam.poste.cheque.domain.model.StatutCheque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository Spring Data sur l'entité JPA. Détail technique, masqué derrière l'adapter. */
public interface DossierChequeJpaRepository extends JpaRepository<DossierChequeJpaEntity, UUID> {

    Optional<DossierChequeJpaEntity> findByReference(String reference);

    /** Filtrage optionnel : un critère nul = pas de restriction sur ce champ. */
    @Query("""
            select d from DossierChequeJpaEntity d
            where (:statut is null or d.statut = :statut)
              and (:agence is null or d.agence = :agence)
            order by d.dateCreation desc
            """)
    List<DossierChequeJpaEntity> rechercher(@Param("statut") StatutCheque statut,
                                            @Param("agence") String agence);
}
