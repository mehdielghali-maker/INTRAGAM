package dz.gam.poste.attestation.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReleveProductionJpaRepository extends JpaRepository<ReleveProductionJpaEntity, UUID> {

    Optional<ReleveProductionJpaEntity> findByReference(String reference);

    Optional<ReleveProductionJpaEntity> findByCodeAgenceAndMois(String codeAgence, String mois);

    List<ReleveProductionJpaEntity> findByCodeAgenceInOrderByDateValidationDesc(List<String> codesAgences);
}
