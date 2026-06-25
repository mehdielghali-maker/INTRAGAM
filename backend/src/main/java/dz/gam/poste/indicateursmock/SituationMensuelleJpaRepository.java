package dz.gam.poste.indicateursmock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SituationMensuelleJpaRepository extends JpaRepository<SituationMensuelleEntity, Long> {

    Optional<SituationMensuelleEntity> findByCodeAgenceAndMois(String codeAgence, String mois);

    List<SituationMensuelleEntity> findByCodeAgenceOrderByMoisDesc(String codeAgence);
}
