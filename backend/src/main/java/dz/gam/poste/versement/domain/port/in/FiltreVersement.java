package dz.gam.poste.versement.domain.port.in;

import dz.gam.poste.versement.domain.model.StatutVersement;

import java.util.Optional;

/**
 * Filtre de recherche des versements. L'agence est OBLIGATOIRE : la liste est toujours
 * bornée à l'agence active (héritée du contexte serveur), jamais d'un paramètre client.
 */
public record FiltreVersement(String codeAgence, StatutVersement statut) {

    public static FiltreVersement parAgence(String codeAgence) {
        return new FiltreVersement(codeAgence, null);
    }

    public FiltreVersement avecStatut(StatutVersement statut) {
        return new FiltreVersement(codeAgence, statut);
    }

    public Optional<StatutVersement> statutFiltre() {
        return Optional.ofNullable(statut);
    }
}
