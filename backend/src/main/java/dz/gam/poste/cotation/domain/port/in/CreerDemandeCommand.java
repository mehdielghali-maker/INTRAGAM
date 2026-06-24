package dz.gam.poste.cotation.domain.port.in;

import java.util.List;

/**
 * Données saisies par l'agence pour une demande. Le code agence, la direction régionale
 * et le créateur ne sont PAS dans la commande : ils proviennent de l'identité SSO (on ne
 * peut pas les usurper). Les pièces jointes sont des noms de fichiers à déposer en GED.
 */
public record CreerDemandeCommand(
        String objet,
        String nomProspect,
        String numeroPolice,
        String commentaire,
        List<String> nomsFichiers) {
}
