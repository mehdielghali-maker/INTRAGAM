package dz.gam.poste.tableaubord.domain.port.out;

import dz.gam.poste.tableaubord.domain.model.Periode;

/**
 * Port de sortie vers Sage (comptabilité) pour les mesures « cash » du tableau de bord.
 * Implémenté par un adapter MOCK aujourd'hui, remplaçable par l'API réelle.
 */
public interface IndicateursSagePort {

    MesuresSage mesurer(Periode periode);
}
