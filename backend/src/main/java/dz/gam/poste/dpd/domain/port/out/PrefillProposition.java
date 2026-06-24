package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.model.InfoClient;
import dz.gam.poste.dpd.domain.model.Souscription;

/** Données pré-remplies depuis une proposition PROASSUR (souscription + client). */
public record PrefillProposition(Souscription souscription, InfoClient client) {
}
