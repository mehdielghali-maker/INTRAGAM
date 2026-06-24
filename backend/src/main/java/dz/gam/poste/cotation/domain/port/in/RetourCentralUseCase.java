package dz.gam.poste.cotation.domain.port.in;

/**
 * Port d'entrée : application des retours du central sur une demande (déclenché par les
 * événements entrants du bus). Identifié par la référence de la demande.
 *
 * <ul>
 *   <li>{@link #prendreEnCharge} : source BPM/OneBase (statut + souscripteur)</li>
 *   <li>{@link #finaliser} et {@link #marquerAffaireGagnee} : source PROASSUR</li>
 * </ul>
 */
public interface RetourCentralUseCase {

    void prendreEnCharge(String reference, String nomSouscripteur);

    void finaliser(String reference, String numeroProposition, String referenceDevis);

    void marquerAffaireGagnee(String reference, String numeroPolice);
}
