package dz.gam.poste.contexte.domain.event;

import java.util.List;

/**
 * Événement applicatif : des agences (codes) viennent d'être déclarées dans le périmètre
 * (au démarrage, ou via l'administration des profils). Les mocks (chiffres, chèques de démo)
 * l'écoutent pour semer les données manquantes de ces agences — sans couplage direct entre
 * modules (pas de cycle).
 */
public record AgencesDeclareesEvent(List<String> codesAgences) {

    public AgencesDeclareesEvent {
        codesAgences = List.copyOf(codesAgences);
    }
}
