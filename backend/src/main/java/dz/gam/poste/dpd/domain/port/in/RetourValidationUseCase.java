package dz.gam.poste.dpd.domain.port.in;

/**
 * Port d'entrée : application des retours du central (déclenché par les événements du bus),
 * identifié par la référence de la demande.
 *
 * <ul>
 *   <li>{@link #mettreEnValidation} : le validateur est lu via le BPM.</li>
 *   <li>{@link #accorder} : l'échéancier validé est lu via PROASSUR (getAccordByCode) et
 *       historisé en version 1.</li>
 * </ul>
 */
public interface RetourValidationUseCase {

    void mettreEnValidation(String reference);

    void accorder(String reference, String codeAccord);

    void refuser(String reference, String motif);
}
