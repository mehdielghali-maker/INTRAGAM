package dz.gam.poste.versement.domain.port.in;

import dz.gam.poste.versement.domain.model.Versement;

import java.util.UUID;

/**
 * Port d'entrée : dépôt d'un versement (brouillon ou soumission au BPM). L'agence et le
 * créateur sont fournis par l'appelant depuis le contexte serveur, jamais par le client.
 */
public interface SoumettreVersementUseCase {

    Versement enregistrerBrouillon(String codeAgence, String createur, DeposerVersementCommand commande);

    Versement soumettre(String codeAgence, String createur, DeposerVersementCommand commande);

    Versement soumettreBrouillon(UUID id);
}
