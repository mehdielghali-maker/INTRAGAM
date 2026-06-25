package dz.gam.poste.indicateursmock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MesuresAgenceJpaRepository extends JpaRepository<MesuresAgenceEntity, String> {

    Optional<MesuresAgenceEntity> findByCodeAgence(String codeAgence);
}
