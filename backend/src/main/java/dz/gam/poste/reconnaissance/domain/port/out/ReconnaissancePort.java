package dz.gam.poste.reconnaissance.domain.port.out;

/**
 * Port « reconnaissance » — capacité INDÉPENDANTE et REMPLAÇABLE.
 *
 * Le poste ne dépend que de cette interface. L'implémentation réelle (service RECO
 * auto-hébergé, ou un autre moteur ANPR) est branchée par configuration (reco.mode) ;
 * en dev on utilise un mock. Changer de fournisseur ne touche QUE l'adapter, jamais le
 * domaine (ADR 0002).
 *
 * Ce port LIT une plaque, il ne juge PAS la conformité : la comparaison avec
 * l'immatriculation du contrat se fait dans
 * {@link dz.gam.poste.reconnaissance.domain.service.VerificationPlaqueService}.
 */
public interface ReconnaissancePort {

    /**
     * @param photo contenu binaire de l'image
     * @param vue   avant / arriere / gauche / droite / toit (peut être null)
     */
    ResultatReco analyser(byte[] photo, String vue);

    /** Résultat brut de la reconnaissance (aucune notion de contrat ici). */
    record ResultatReco(
            boolean estVehicule,
            String typeVehicule,     // "voiture", "camion", ... ou null
            String plaque,           // plaque normalisée, ou null si non lue
            double confiance,        // confiance de la lecture de plaque [0..1]
            double confianceVehicule // confiance de la détection véhicule [0..1]
    ) {}
}
