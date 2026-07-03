package dz.gam.poste.versement.adapter.out.persistence;

import dz.gam.poste.versement.domain.model.StatutVersement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersementJpaRepository extends JpaRepository<VersementJpaEntity, UUID> {

    Optional<VersementJpaEntity> findByReference(String reference);

    long countByReferenceIsNotNull();

    long countByStatutIn(List<StatutVersement> statuts);

    @Query("""
            select v from VersementJpaEntity v
            where v.codeAgence = :codeAgence
              and (:statut is null or v.statut = :statut)
            order by v.dateCreation desc
            """)
    List<VersementJpaEntity> rechercher(@Param("codeAgence") String codeAgence,
                                        @Param("statut") StatutVersement statut);
}
