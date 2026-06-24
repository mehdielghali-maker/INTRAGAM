package dz.gam.poste.cotation.adapter.out.persistence;

import dz.gam.poste.cotation.domain.model.StatutCotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeCotationJpaRepository extends JpaRepository<DemandeCotationJpaEntity, UUID> {

    Optional<DemandeCotationJpaEntity> findByReference(String reference);

    long countByReferenceIsNotNull();

    @Query("""
            select d from DemandeCotationJpaEntity d
            where (:statut is null or d.statut = :statut)
            order by d.dateCreation desc
            """)
    List<DemandeCotationJpaEntity> rechercher(@Param("statut") StatutCotation statut);
}
