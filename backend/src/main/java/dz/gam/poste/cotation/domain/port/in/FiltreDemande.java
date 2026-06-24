package dz.gam.poste.cotation.domain.port.in;

import dz.gam.poste.cotation.domain.model.StatutCotation;

import java.util.Optional;

/** Critère de filtrage de la liste des demandes (statut optionnel). */
public record FiltreDemande(Optional<StatutCotation> statut) {

    public static FiltreDemande aucun() {
        return new FiltreDemande(Optional.empty());
    }

    public static FiltreDemande parStatut(StatutCotation statut) {
        return new FiltreDemande(Optional.ofNullable(statut));
    }
}
