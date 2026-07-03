package dz.gam.poste.attestation.domain.port.out;

import dz.gam.poste.attestation.domain.model.ResultatAttestation;

/**
 * Port « lecture de document » — capacité OCR INDÉPENDANTE et REMPLAÇABLE (même principe
 * que la reconnaissance véhicule, ADR 0011). Le poste ne dépend que de cette interface ;
 * l'implémentation réelle (OCR MUTUALISÉ dans le conteneur reco, endpoint
 * {@code POST /lire-attestation}) est branchée par configuration ({@code ocr.mode}) ;
 * en dev on utilise un mock. Changer de moteur OCR ne touche QUE l'adapter.
 *
 * Ce port LIT une attestation, il ne juge PAS la production : la constitution du relevé
 * et ses gardes vivent dans le domaine.
 */
public interface LectureDocumentPort {

    /**
     * @param photo contenu binaire de l'image (photo ou scan de l'attestation)
     * @return le résultat de lecture ; JAMAIS null — en cas d'échec l'adapter renvoie
     *         {@link ResultatAttestation#neutre()} (la panne de l'OCR ne bloque pas la collecte)
     */
    ResultatAttestation lireAttestation(byte[] photo);
}
