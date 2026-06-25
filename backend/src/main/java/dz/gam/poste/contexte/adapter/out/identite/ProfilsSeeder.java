package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Au démarrage : initialise les profils persistés depuis la configuration (si la base est
 * vide), puis publie {@link AgencesDeclareesEvent} pour que les mocks sèment les chiffres et
 * chèques de démo des agences déclarées. Source unique de déclenchement du seeding.
 */
@Component
@Order(1) // avant tout ce qui dépend des profils/agences
public class ProfilsSeeder implements ApplicationRunner {

    private final ContexteProperties properties;
    private final ProfilsAdminStore store;
    private final ApplicationEventPublisher evenements;

    public ProfilsSeeder(ContexteProperties properties, ProfilsAdminStore store,
                         ApplicationEventPublisher evenements) {
        this.properties = properties;
        this.store = store;
        this.evenements = evenements;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (store.estVide()) {
            properties.profils().forEach(store::enregistrer);
        }
        List<String> codes = store.tousLesCodesAgences();
        if (!codes.isEmpty()) {
            evenements.publishEvent(new AgencesDeclareesEvent(codes));
        }
    }
}
