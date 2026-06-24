package dz.gam.poste.dpd.domain.port.in;

import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.port.out.AccordProassur;

import java.util.Optional;

/** Port d'entrée : onglet « Mise à jour DPD ». */
public interface MiseAJourDpdUseCase {

    /** « Charger depuis PROASSUR » : résumé + échéancier validé (lecture seule). Vide si inconnu. */
    Optional<AccordProassur> chargerAccord(String codeAccord);

    /**
     * « Mettre à jour le dossier » : relit l'accord chez PROASSUR, historise une nouvelle
     * version de l'échéancier côté poste et publie {@code EcheancierDpdMisAJour}.
     */
    AccordSuivi synchroniser(String codeAccord);

    /** Variante avec le motif (commentaire) de la mise à jour, conservé sur la version. */
    AccordSuivi synchroniser(String codeAccord, String commentaire);
}
