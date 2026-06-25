package dz.gam.poste.contexte.adapter.out.identite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfilAgaJpaRepository extends JpaRepository<ProfilAgaEntity, String> {

    Optional<ProfilAgaEntity> findByLogin(String login);
}
