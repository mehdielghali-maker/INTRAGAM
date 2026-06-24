package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.model.AccordSuivi;

import java.util.Optional;

/** Port de sortie : persistance du suivi versionné des accords (propre au poste). */
public interface AccordSuiviRepository {

    AccordSuivi enregistrer(AccordSuivi accord);

    Optional<AccordSuivi> trouverParCodeAccord(String codeAccord);
}
