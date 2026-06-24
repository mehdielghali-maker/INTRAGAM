package dz.gam.poste.dpd.domain.port.in;

import dz.gam.poste.dpd.domain.model.StatutDpd;

import java.util.Optional;

/** Critère de filtrage des demandes DPD (statut optionnel). */
public record FiltreDpd(Optional<StatutDpd> statut) {

    public static FiltreDpd aucun() {
        return new FiltreDpd(Optional.empty());
    }

    public static FiltreDpd parStatut(StatutDpd statut) {
        return new FiltreDpd(Optional.ofNullable(statut));
    }
}
