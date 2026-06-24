package dz.gam.poste.dpd.domain.model;

import dz.gam.poste.dpd.domain.event.EcheancierDpdMisAJourEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Suivi versionné d'un accord côté poste : on conserve les échéanciers successifs (jamais
 * d'écrasement) pour la traçabilité des renégociations. L'échéancier lui-même reste la
 * propriété de PROASSUR ; le poste n'en garde qu'un reflet daté.
 */
public class AccordSuivi {

    private final UUID id;
    private final String codeAccord;
    private ResumeAccord resume;
    private final List<EcheancierVersion> versions;
    private Instant dateMaj;

    private final transient List<EcheancierDpdMisAJourEvent> evenements = new ArrayList<>();

    private AccordSuivi(UUID id, String codeAccord, ResumeAccord resume,
                        List<EcheancierVersion> versions, Instant dateMaj) {
        this.id = Objects.requireNonNull(id);
        this.codeAccord = Objects.requireNonNull(codeAccord);
        this.resume = Objects.requireNonNull(resume);
        this.versions = new ArrayList<>(versions);
        this.dateMaj = Objects.requireNonNull(dateMaj);
    }

    /** Crée le suivi avec sa première version d'échéancier (aucun événement). */
    public static AccordSuivi creer(String codeAccord, ResumeAccord resume, List<Echeance> echeances, Instant maintenant) {
        EcheancierVersion v1 = new EcheancierVersion(1, maintenant, echeances);
        return new AccordSuivi(UUID.randomUUID(), codeAccord, resume, new ArrayList<>(List.of(v1)), maintenant);
    }

    public static AccordSuivi reconstituer(UUID id, String codeAccord, ResumeAccord resume,
                                           List<EcheancierVersion> versions, Instant dateMaj) {
        return new AccordSuivi(id, codeAccord, resume, versions, dateMaj);
    }

    /** Ajoute une nouvelle version (renégociation) sans écraser les précédentes ; émet l'événement. */
    public void mettreAJour(ResumeAccord nouveauResume, List<Echeance> echeances, Instant maintenant) {
        int prochaineVersion = versions.size() + 1;
        versions.add(new EcheancierVersion(prochaineVersion, maintenant, echeances));
        this.resume = nouveauResume;
        this.dateMaj = maintenant;
        evenements.add(new EcheancierDpdMisAJourEvent(codeAccord, prochaineVersion, maintenant));
    }

    public EcheancierVersion versionCourante() {
        return versions.get(versions.size() - 1);
    }

    public List<EcheancierDpdMisAJourEvent> evenementsNonPublies() {
        return Collections.unmodifiableList(evenements);
    }

    public void viderEvenements() {
        evenements.clear();
    }

    public UUID id() {
        return id;
    }

    public String codeAccord() {
        return codeAccord;
    }

    public ResumeAccord resume() {
        return resume;
    }

    public List<EcheancierVersion> versions() {
        return Collections.unmodifiableList(versions);
    }

    public Instant dateMaj() {
        return dateMaj;
    }
}
