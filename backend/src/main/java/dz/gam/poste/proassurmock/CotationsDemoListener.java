package dz.gam.poste.proassurmock;

import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.ReferenceDemande;
import dz.gam.poste.cotation.domain.model.Souscripteur;
import dz.gam.poste.cotation.domain.port.out.DemandeCotationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Sème quelques demandes de cotation FICTIVES par agence (simule un suivi PROASSUR/BPM) en
 * réaction à {@link AgencesDeclareesEvent} — au démarrage et à l'ajout d'une agence via l'admin —
 * afin que la liste « Demandes de cotation » soit peuplée par agence pour la session de l'AGA.
 *
 * <p>3 demandes par agence avec des références déterministes ({@code DC-{agence}-001/002/003})
 * et des statuts VARIÉS (ENVOYEE, EN_COURS, A_FINALISER). L'événement étant rejoué, le seed est
 * IDEMPOTENT : on saute toute demande déjà présente (recherche par référence) avant de la semer.
 */
@Component
public class CotationsDemoListener {

    private static final String DIRECTION_REGIONALE = "DR Alger-Centre";

    /** Gabarits de démo : objet, prospect, n° police (renouvellement si non vide), souscripteur. */
    private record Gabarit(String objet, String nomProspect, String numeroPolice, String souscripteur) {
    }

    private static final List<Gabarit> GABARITS = List.of(
            new Gabarit("Auto — flotte VTC", "SARL Trans-Med", null, "K. Bensalem"),
            new Gabarit("Multirisque professionnelle", "EURL Baticonstruct", "P-20381", "A. Cherifi"),
            new Gabarit("Auto — particulier", "M. Belkacem Yacine", null, "S. Hamidi"));

    private final DemandeCotationRepository repository;
    private final Clock horloge;

    public CotationsDemoListener(DemandeCotationRepository repository, Clock horloge) {
        this.repository = repository;
        this.horloge = horloge;
    }

    @EventListener
    public void surAgencesDeclarees(AgencesDeclareesEvent evenement) {
        for (String agence : evenement.codesAgences()) {
            semerPourAgence(agence);
        }
    }

    private void semerPourAgence(String agence) {
        for (int n = 1; n <= GABARITS.size(); n++) {
            ReferenceDemande reference = new ReferenceDemande("DC-%s-%03d".formatted(agence, n));
            if (repository.trouverParReference(reference).isPresent()) {
                continue; // déjà semée : ne pas dupliquer (idempotence)
            }
            Instant maintenant = horloge.instant();
            Gabarit gabarit = GABARITS.get(n - 1);

            DemandeCotation demande = DemandeCotation.creerBrouillon(
                    gabarit.objet(), gabarit.nomProspect(), gabarit.numeroPolice(),
                    "Demande de démo (AGA)", agence, DIRECTION_REGIONALE, "Démo PROASSUR",
                    List.of(), maintenant);

            demande.envoyer(reference, maintenant);
            demande.viderEvenements(); // pas de publication bus pour la donnée de démo

            // Statuts variés : 1 -> ENVOYEE, 2 -> EN_COURS, 3 -> A_FINALISER
            if (n >= 2) {
                demande.prendreEnCharge(new Souscripteur(gabarit.souscripteur()), maintenant);
            }
            if (n >= 3) {
                demande.finaliser("PR-%s-%03d".formatted(agence, n), "DV-%s-%03d".formatted(agence, n), maintenant);
            }

            repository.enregistrer(demande);
        }
    }
}
