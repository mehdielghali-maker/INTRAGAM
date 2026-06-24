package dz.gam.poste.dpd.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Une VERSION de l'échéancier validé (traçabilité des renégociations : on n'écrase jamais
 * l'ancienne). Calcule les sous-totaux réglé / restant pour le récap.
 */
public record EcheancierVersion(int version, Instant dateValidation, String commentaire, List<Echeance> echeances) {

    public EcheancierVersion {
        echeances = List.copyOf(echeances);
    }

    public BigDecimal total() {
        return echeances.stream().map(Echeance::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long nbReglees() {
        return echeances.stream().filter(Echeance::estReglee).count();
    }

    public BigDecimal montantRegle() {
        return echeances.stream().filter(Echeance::estReglee)
                .map(Echeance::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long nbRestantes() {
        return echeances.size() - nbReglees();
    }

    public BigDecimal montantRestant() {
        return total().subtract(montantRegle());
    }
}
