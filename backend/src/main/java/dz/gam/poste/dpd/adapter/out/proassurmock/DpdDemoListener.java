package dz.gam.poste.dpd.adapter.out.proassurmock;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.InfoClient;
import dz.gam.poste.dpd.domain.model.PieceJointeDpd;
import dz.gam.poste.dpd.domain.model.ReferenceDpd;
import dz.gam.poste.dpd.domain.model.Souscription;
import dz.gam.poste.dpd.domain.model.TypePersonne;
import dz.gam.poste.dpd.domain.model.TypePiece;
import dz.gam.poste.dpd.domain.model.Validateur;
import dz.gam.poste.dpd.domain.port.out.AccordProassur;
import dz.gam.poste.dpd.domain.port.out.AccordSuiviRepository;
import dz.gam.poste.dpd.domain.port.out.DemandeDpdRepository;
import dz.gam.poste.dpd.domain.port.out.GedDpdPort;
import dz.gam.poste.dpd.domain.port.out.ProassurDpdPort;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Sème 3 accords d'échéancier (DPD) FICTIFS par agence en réaction à
 * {@link AgencesDeclareesEvent} — au démarrage et à l'ajout d'une agence via l'admin — afin que
 * l'écran « Accords d'échéancier » soit peuplé par agence pour la session de l'AGA.
 *
 * <p>Statuts VARIÉS : une demande ENVOYEE, une EN_VALIDATION, une ACCORDEE (avec son suivi
 * d'accord + échéancier). Le seed reconstruit le cycle de vie réel de l'agrégat (dépôt RC en
 * GED → envoyer → mettre en validation → accorder), donc il passe par les ports, jamais par du
 * « tout-en-dur ».
 *
 * <p>IDEMPOTENT : l'événement est REJOUÉ (démarrage + chaque enregistrement de profil). La
 * référence DPD est déterministe ({@code DE-DEMO-<agence>-00n}) et une garde par référence
 * empêche toute duplication avant de semer quoi que ce soit.
 */
@Component
public class DpdDemoListener {

    /** Wilaya / direction régionale de démonstration. */
    private static final String WILAYA = "Alger";
    private static final String DR = "DR Alger-Est";
    private static final String MAIL_DR = "dr.alger-est@gam.dz";
    private static final String CREATEUR = "M. Benzerga";

    private final DemandeDpdRepository demandes;
    private final AccordSuiviRepository accords;
    private final GedDpdPort ged;
    private final ProassurDpdPort proassur;
    private final Clock horloge;

    public DpdDemoListener(DemandeDpdRepository demandes, AccordSuiviRepository accords,
                           GedDpdPort ged, ProassurDpdPort proassur, Clock horloge) {
        this.demandes = demandes;
        this.accords = accords;
        this.ged = ged;
        this.proassur = proassur;
        this.horloge = horloge;
    }

    @EventListener
    public void surAgencesDeclarees(AgencesDeclareesEvent evenement) {
        for (String agence : evenement.codesAgences()) {
            // n=1 → ENVOYEE, n=2 → EN_VALIDATION, n=3 → ACCORDEE (+ suivi d'accord).
            semer(agence, 1, Cible.ENVOYEE);
            semer(agence, 2, Cible.EN_VALIDATION);
            semer(agence, 3, Cible.ACCORDEE);
        }
    }

    private enum Cible {ENVOYEE, EN_VALIDATION, ACCORDEE}

    private void semer(String agence, int n, Cible cible) {
        ReferenceDpd reference = new ReferenceDpd("DE-DEMO-%s-%03d".formatted(agence, n));
        // Garde d'idempotence AVANT toute écriture (référence déterministe).
        if (demandes.trouverParReference(reference).isPresent()) {
            return;
        }
        Instant maintenant = horloge.instant();

        // Pièce RC obligatoire déposée en GED OneBase avant l'envoi (le port renvoie la référence).
        PieceJointeDpd rc = ged.deposer(TypePiece.RC, "rc.pdf");

        DemandeDpd demande = DemandeDpd.creerBrouillon(
                souscription(n), infoClient(n), false, "DPD démo — Auto, flotte 6 mois",
                agence, mailAgence(agence), DR, MAIL_DR, CREATEUR, List.of(rc), maintenant);

        demande.envoyer(reference, maintenant);

        if (cible == Cible.EN_VALIDATION || cible == Cible.ACCORDEE) {
            demande.mettreEnValidation(new Validateur("Validateur DR"), maintenant);
        }

        String codeAccord = "AC-DEMO-%s-%03d".formatted(agence, n);
        if (cible == Cible.ACCORDEE) {
            demande.accorder(codeAccord, maintenant);
        }

        demande.viderEvenements(); // démo : pas de publication sur le bus
        demandes.enregistrer(demande);

        // Pour une demande accordée, on sème aussi le suivi de l'accord (résumé + échéancier PROASSUR).
        if (cible == Cible.ACCORDEE && accords.trouverParCodeAccord(codeAccord).isEmpty()) {
            Optional<AccordProassur> accord = proassur.getAccordByCode(codeAccord);
            accord.ifPresent(a -> accords.enregistrer(
                    AccordSuivi.creer(codeAccord, a.resume(), a.echeances(), "Accord initial (démo)", maintenant)));
        }
    }

    /** Souscription PROASSUR (lecture seule) — prime ~1 240 000 DA, produit « Auto — flotte ». */
    private Souscription souscription(int n) {
        return new Souscription(
                "PR-DEMO-%03d".formatted(n),
                LocalDate.of(2026, 6, 1),
                CREATEUR,
                new BigDecimal("1240000"),
                "Auto — flotte",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2027, 6, 30),
                6,
                null);
    }

    /** Info client de démo — personnes morales algériennes plausibles. */
    private InfoClient infoClient(int n) {
        String[] noms = {"SARL Méditerranée Logistic", "EURL Trans-Med", "SPA Yassir Distribution"};
        String nom = noms[(n - 1) % noms.length];
        return new InfoClient(nom, nom, "021 55 66 77", "16/00-1234567 B 09",
                TypePersonne.MORALE, false, "Zone industrielle, Rouiba, " + WILAYA);
    }

    private String mailAgence(String agence) {
        return "agence." + agence.toLowerCase().replace(".", "-").replace(" ", "-") + "@gam.dz";
    }
}
