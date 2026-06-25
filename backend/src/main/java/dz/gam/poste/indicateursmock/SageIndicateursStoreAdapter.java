package dz.gam.poste.indicateursmock;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.port.out.IndicateursSagePort;
import dz.gam.poste.tableaubord.domain.port.out.MesuresSage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter Sage (mesures « cash » de l'accueil) lisant le store éditable (substitut du Cube
 * Power BI). À remplacer par l'adapter réel sans toucher au domaine.
 */
@Component
public class SageIndicateursStoreAdapter implements IndicateursSagePort {

    private final MesuresAgenceJpaRepository repository;

    public SageIndicateursStoreAdapter(MesuresAgenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public MesuresSage mesurer(String codeAgence, Periode periode) {
        return repository.findByCodeAgence(codeAgence)
                .map(e -> new MesuresSage(e.deposeMois, e.encaissementsLettres, e.encaissementsLettresM1))
                .orElseGet(() -> new MesuresSage(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
