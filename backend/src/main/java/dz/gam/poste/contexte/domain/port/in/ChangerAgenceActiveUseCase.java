package dz.gam.poste.contexte.domain.port.in;

import dz.gam.poste.contexte.domain.model.ContexteAgence;

/**
 * Port d'entrée : changement d'agence active. Le code reçu est revérifié contre le
 * périmètre SSO ; hors périmètre → {@code AgenceHorsPerimetreException} (HTTP 403).
 */
public interface ChangerAgenceActiveUseCase {

    ContexteAgence changer(String codeAgence);
}
