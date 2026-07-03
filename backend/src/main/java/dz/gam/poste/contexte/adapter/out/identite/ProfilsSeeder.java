package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.contexte.domain.model.CompteAdmin;
import dz.gam.poste.contexte.domain.model.Modules;
import dz.gam.poste.contexte.domain.port.out.CompteAdminStore;
import dz.gam.poste.contexte.domain.port.out.MotDePasseEncodeur;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Au démarrage : initialise les profils persistés depuis la configuration (si la base est
 * vide), crée le compte admin par défaut (admin/admin) s'il n'existe pas, puis publie
 * {@link AgencesDeclareesEvent} pour que les mocks sèment les chiffres et chèques de démo des
 * agences déclarées. Source unique de déclenchement du seeding.
 */
@Component
@Order(1) // avant tout ce qui dépend des profils/agences
public class ProfilsSeeder implements ApplicationRunner {

    /** Mot de passe admin par défaut au tout premier démarrage (à changer ensuite dans l'admin). */
    private static final String MOT_DE_PASSE_ADMIN_DEFAUT = "admin";
    private static final String LOGIN_ADMIN = "admin";

    private final ContexteProperties properties;
    private final ProfilsAdminStore store;
    private final CompteAdminStore compteAdmin;
    private final MotDePasseEncodeur encodeur;
    private final ApplicationEventPublisher evenements;

    public ProfilsSeeder(ContexteProperties properties, ProfilsAdminStore store,
                         CompteAdminStore compteAdmin, MotDePasseEncodeur encodeur,
                         ApplicationEventPublisher evenements) {
        this.properties = properties;
        this.store = store;
        this.compteAdmin = compteAdmin;
        this.encodeur = encodeur;
        this.evenements = evenements;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (store.estVide()) {
            properties.profils().forEach(p -> {
                List<String> modules = (p.modules() == null || p.modules().isEmpty()) ? Modules.TOUS : p.modules();
                store.enregistrer(new ContexteProperties.Profil(
                        p.identifiant(), p.login(), p.nomAffiche(), p.profil(), p.agences(), modules, p.motDePasse()));
            });
        }
        if (compteAdmin.charger().isEmpty()) {
            compteAdmin.enregistrer(new CompteAdmin(
                    LOGIN_ADMIN, encodeur.encoder(MOT_DE_PASSE_ADMIN_DEFAUT), null));
        }
        List<String> codes = store.tousLesCodesAgences();
        if (!codes.isEmpty()) {
            try {
                evenements.publishEvent(new AgencesDeclareesEvent(codes));
            } catch (RuntimeException e) {
                // Le seeding de démo est un CONFORT : il ne doit jamais empêcher le démarrage.
                // (Les listeners sont idempotents : le rejeu suivant rattrapera le manque.)
                LoggerFactory.getLogger(ProfilsSeeder.class)
                        .warn("Seeding des données de démo interrompu : {}", e.getMessage());
            }
        }
    }
}
