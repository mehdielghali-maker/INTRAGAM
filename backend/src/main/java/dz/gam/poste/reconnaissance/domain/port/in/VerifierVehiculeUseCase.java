package dz.gam.poste.reconnaissance.domain.port.in;

import dz.gam.poste.reconnaissance.domain.model.ResultatVerificationVehicule;

/**
 * Port d'entrée : analyser une photo de véhicule puis comparer la plaque lue à
 * l'immatriculation du contrat. La reconnaissance est un CONFORT — une panne du
 * service ne bloque jamais le poste (l'adapter renvoie alors un résultat neutre).
 */
public interface VerifierVehiculeUseCase {

    /**
     * @param photo           contenu binaire de l'image
     * @param vue             avant / arriere / gauche / droite / toit (peut être null)
     * @param immatriculation immatriculation pré-remplie depuis PROASSUR (obligatoire)
     */
    ResultatVerificationVehicule verifier(byte[] photo, String vue, String immatriculation);
}
