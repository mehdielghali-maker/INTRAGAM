package dz.gam.poste.dpd.adapter.out.persistence;

import dz.gam.poste.dpd.domain.model.StatutDpd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeDpdJpaRepository extends JpaRepository<DemandeDpdJpaEntity, UUID> {

    Optional<DemandeDpdJpaEntity> findByReference(String reference);

    long countByReferenceIsNotNull();

    @Query("""
            select d from DemandeDpdJpaEntity d
            where (:statut is null or d.statut = :statut)
            order by d.dateMaj desc
            """)
    List<DemandeDpdJpaEntity> rechercher(@Param("statut") StatutDpd statut);
}
