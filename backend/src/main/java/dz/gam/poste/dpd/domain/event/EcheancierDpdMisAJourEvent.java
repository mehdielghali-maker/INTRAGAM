package dz.gam.poste.dpd.domain.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement de domaine émis quand un échéancier revalidé est synchronisé (onglet « Mise à
 * jour DPD »). Notifie « Suivi des échéanciers » que l'état suivi a changé.
 */
public record EcheancierDpdMisAJourEvent(String codeAccord, int version, Instant dateMaj) {

    public EcheancierDpdMisAJourEvent {
        Objects.requireNonNull(codeAccord, "codeAccord obligatoire");
        Objects.requireNonNull(dateMaj, "dateMaj obligatoire");
    }
}
