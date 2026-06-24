package dz.gam.poste.dpd.domain.port.in;

import dz.gam.poste.dpd.domain.model.DemandeDpd;

import java.util.UUID;

/** Port d'entrée : création / envoi des demandes DPD par l'agence. */
public interface EnregistrerDpdUseCase {

    DemandeDpd enregistrerBrouillon(CreerDpdCommand commande);

    /** Crée et envoie (bouton « Envoyer pour validation ») ; exige le RC. */
    DemandeDpd envoyer(CreerDpdCommand commande);

    DemandeDpd envoyerBrouillon(UUID idDemande);
}
