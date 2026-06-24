package dz.gam.poste.dpd.adapter.messaging;

/** Contrat de message « EcheancierDpdMisAJour » (poste → Suivi des échéanciers). */
public record EcheancierDpdMisAJourMessage(String codeAccord, int version) {
}
