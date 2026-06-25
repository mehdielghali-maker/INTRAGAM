package dz.gam.poste.contexte.adapter.out.securite;

import dz.gam.poste.contexte.domain.port.out.MotDePasseEncodeur;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implémentation BCrypt du {@link MotDePasseEncodeur}. Le sel est intégré à l'empreinte ;
 * {@link #correspond} gère les empreintes nulles (aucun mot de passe défini → jamais valide).
 */
@Component
public class BCryptMotDePasseEncodeur implements MotDePasseEncodeur {

    private final BCryptPasswordEncoder delegue = new BCryptPasswordEncoder();

    @Override
    public String encoder(String motDePasseClair) {
        return delegue.encode(motDePasseClair);
    }

    @Override
    public boolean correspond(String motDePasseClair, String empreinte) {
        if (empreinte == null || empreinte.isBlank()) {
            return false;
        }
        return delegue.matches(motDePasseClair, empreinte);
    }
}
