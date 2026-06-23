package dz.gam.poste.tableaubord.domain.port.out;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.PerimetreSP;

/**
 * Port de sortie vers PROASSUR pour les mesures du tableau de bord. Implémenté
 * aujourd'hui par un adapter MOCK, remplaçable par l'API réelle sans toucher au
 * domaine (même principe que la fonction « Suivi des chèques »).
 */
public interface IndicateursProassurPort {

    MesuresProassur mesurer(Periode periode, PerimetreSP perimetreSP);
}
