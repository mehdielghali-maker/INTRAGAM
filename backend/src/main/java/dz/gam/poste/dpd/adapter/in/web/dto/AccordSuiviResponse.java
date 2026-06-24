package dz.gam.poste.dpd.adapter.in.web.dto;

import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.EcheancierVersion;
import dz.gam.poste.dpd.domain.model.ResumeAccord;

import java.math.BigDecimal;

/** Vue REST du suivi d'un accord après synchronisation : résumé + version courante + récap. */
public record AccordSuiviResponse(
        String codeAccord,
        ResumeAccord resume,
        int versionCourante,
        int nbVersions,
        BigDecimal total,
        long nbReglees,
        BigDecimal montantRegle,
        long nbRestantes,
        BigDecimal montantRestant) {

    public static AccordSuiviResponse de(AccordSuivi a) {
        EcheancierVersion v = a.versionCourante();
        return new AccordSuiviResponse(
                a.codeAccord(), a.resume(), v.version(), a.versions().size(),
                v.total(), v.nbReglees(), v.montantRegle(), v.nbRestantes(), v.montantRestant());
    }
}
