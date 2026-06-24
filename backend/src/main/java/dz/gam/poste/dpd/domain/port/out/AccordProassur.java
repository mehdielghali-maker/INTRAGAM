package dz.gam.poste.dpd.domain.port.out;

import dz.gam.poste.dpd.domain.model.Echeance;
import dz.gam.poste.dpd.domain.model.ResumeAccord;

import java.util.List;

/** Vue d'un accord renvoyée par PROASSUR : résumé + échéancier validé avec états de règlement. */
public record AccordProassur(ResumeAccord resume, List<Echeance> echeances) {
}
