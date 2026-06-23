package dz.gam.poste.tableaubord.domain.port.in;

import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;

/**
 * Port d'entrée : fournir les compteurs de badges de la navigation (sidebar), présents
 * sur tout l'écran. Le compteur des chèques est réel ; les autres sont mockés.
 */
public interface ConsulterNavigationUseCase {

    CompteursAgence badges();
}
