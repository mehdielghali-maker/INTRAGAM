package dz.gam.poste.dpd.domain.port.out;

import java.util.Optional;

/**
 * Port de sortie vers PROASSUR pour le DPD. Mock en dev, remplaçable par l'API réelle.
 * Le module lit/transporte ; il ne recalcule ni la souscription ni l'échéancier.
 */
public interface ProassurDpdPort {

    /** Pré-remplit la demande à partir d'une proposition. Vide si la proposition est inconnue. */
    Optional<PrefillProposition> prefillFromProposition(String noProposition);

    /** Résumé + échéancier validé (avec états de règlement) d'un accord. Vide si inconnu. */
    Optional<AccordProassur> getAccordByCode(String codeAccord);
}
