package dz.gam.poste.versement.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologie du bus pour le versement, sur l'exchange topic commun du poste (injecté). Deux
 * files : une consommée par le BPM (mock), une par le poste (retours de statut).
 */
@Configuration
public class VersementRabbitConfig {

    @Bean
    public Queue queueVersementDepose() {
        return QueueBuilder.durable(VersementBus.QUEUE_VERSEMENT_DEPOSE).build();
    }

    @Bean
    public Queue queueVersementStatut() {
        return QueueBuilder.durable(VersementBus.QUEUE_VERSEMENT_STATUT).build();
    }

    @Bean
    public Binding bindingVersementDepose(Queue queueVersementDepose, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueVersementDepose).to(evenementsExchange).with(VersementBus.RK_VERSEMENT_DEPOSE);
    }

    @Bean
    public Binding bindingVersementStatut(Queue queueVersementStatut, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueVersementStatut).to(evenementsExchange).with(VersementBus.RK_VERSEMENT_STATUT);
    }
}
