package dz.gam.poste.cotation.domain.port.in;

import dz.gam.poste.cotation.domain.model.DemandeCotation;

import java.util.UUID;

/** Port d'entrée : création / envoi des demandes par l'agence. */
public interface EnregistrerDemandeUseCase {

    DemandeCotation enregistrerBrouillon(CreerDemandeCommand commande);

    /** Crée et envoie en une fois (bouton « Envoyer la demande »). */
    DemandeCotation envoyer(CreerDemandeCommand commande);

    /** Envoie un brouillon existant. */
    DemandeCotation envoyerBrouillon(UUID idDemande);
}
