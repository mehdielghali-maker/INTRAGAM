package dz.gam.poste.versement.domain.port.in;

/**
 * Port d'entrée : application des retours de statut du BPM (boucle fermée).
 * Déposé → En contrôle → Validé / Rejeté. Aucune règle métier côté adapter.
 */
public interface RetourBpmUseCase {

    void mettreEnControle(String reference, String referenceBpm);

    void valider(String reference);

    void rejeter(String reference, String motif);
}
