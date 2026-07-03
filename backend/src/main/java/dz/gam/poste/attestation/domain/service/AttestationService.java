package dz.gam.poste.attestation.domain.service;

import dz.gam.poste.attestation.domain.model.ReleveDejaValideException;
import dz.gam.poste.attestation.domain.model.ReleveProduction;
import dz.gam.poste.attestation.domain.model.ResultatAttestation;
import dz.gam.poste.attestation.domain.port.in.ConsulterRelevesUseCase;
import dz.gam.poste.attestation.domain.port.in.LireAttestationUseCase;
import dz.gam.poste.attestation.domain.port.in.SoumettreReleveCommand;
import dz.gam.poste.attestation.domain.port.in.SoumettreReleveUseCase;
import dz.gam.poste.attestation.domain.port.out.LectureDocumentPort;
import dz.gam.poste.attestation.domain.port.out.ProductionPort;
import dz.gam.poste.attestation.domain.port.out.ReleveProductionRepository;
import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;

import java.time.Clock;
import java.util.List;

/**
 * Service applicatif des « Attestations » (relevé de production). Java pur.
 *
 * L'agence vient TOUJOURS du contexte serveur : {@code agencePourAction()} pour la
 * soumission (consolidé → 409 via l'exception transverse), {@code agencesActives()} pour
 * borner la consultation au périmètre. Garde métier : un seul relevé validé par
 * (agence, mois) — re-soumission → {@link ReleveDejaValideException} (HTTP 409).
 *
 * Arbitrage validé : COLLECTE SEULE — la vérification PROASSUR police par police est un
 * seam prévu dans {@link ProductionPort} (commenté), non branché dans cette tranche.
 */
public class AttestationService implements LireAttestationUseCase, SoumettreReleveUseCase,
        ConsulterRelevesUseCase {

    private final LectureDocumentPort lecture;
    private final ProductionPort production;
    private final ReleveProductionRepository repository;
    private final AgenceCouranteQuery agenceCourante;
    private final Clock horloge;

    public AttestationService(LectureDocumentPort lecture, ProductionPort production,
                              ReleveProductionRepository repository, AgenceCouranteQuery agenceCourante,
                              Clock horloge) {
        this.lecture = lecture;
        this.production = production;
        this.repository = repository;
        this.agenceCourante = agenceCourante;
        this.horloge = horloge;
    }

    @Override
    public ResultatAttestation lire(byte[] photo) {
        if (photo == null || photo.length == 0) {
            throw new IllegalArgumentException("La photo de l'attestation est obligatoire.");
        }
        return lecture.lireAttestation(photo);
    }

    @Override
    public RecuReleve soumettre(SoumettreReleveCommand commande) {
        // Une soumission vise une agence PRÉCISE : en consolidé, agencePourAction() lève (409).
        Agence agence = agenceCourante.agencePourAction();

        // Garde « déjà validé » : un lot par (agence, mois) ; après validation → verrouillé.
        repository.trouverParAgenceEtMois(agence.code(), commande.mois()).ifPresent(existant -> {
            throw new ReleveDejaValideException(agence.code(), commande.mois());
        });

        ReleveProduction releve = ReleveProduction.valider(
                commande.mois(), agence.code(), commande.lignes(), horloge.instant());

        // Seam PROASSUR/Sage : le mock acquitte ; un adapter réel transmettra au central.
        ProductionPort.RetourProduction retour = production.soumettre(releve);
        repository.enregistrer(releve);

        return new RecuReleve(releve.reference(), releve.mois(), releve.codeAgence(),
                releve.nombreLignes(), retour.message());
    }

    @Override
    public List<ReleveProduction> relevesValides() {
        List<String> codes = agenceCourante.agencesActives().stream().map(Agence::code).toList();
        return repository.listerPourAgences(codes);
    }
}
