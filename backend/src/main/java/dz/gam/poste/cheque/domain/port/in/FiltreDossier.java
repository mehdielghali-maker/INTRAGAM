package dz.gam.poste.cheque.domain.port.in;

import dz.gam.poste.cheque.domain.model.StatutCheque;

import java.util.Optional;

/**
 * Critères de filtrage de la liste des dossiers. Chaque critère est optionnel ;
 * absent = pas de restriction sur ce champ.
 */
public record FiltreDossier(Optional<StatutCheque> statut, Optional<String> agence) {

    public static FiltreDossier aucun() {
        return new FiltreDossier(Optional.empty(), Optional.empty());
    }

    public static FiltreDossier de(StatutCheque statut, String agence) {
        return new FiltreDossier(
                Optional.ofNullable(statut),
                Optional.ofNullable(agence).filter(a -> !a.isBlank()));
    }
}
