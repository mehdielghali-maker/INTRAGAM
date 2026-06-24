package dz.gam.poste.dpd.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Topologie du bus DPD, sur l'exchange topic commun du poste (injecté). */
@Configuration
public class DpdRabbitConfig {

    @Bean
    public Queue queueDpdEmise() {
        return QueueBuilder.durable(DpdBus.QUEUE_DEMANDE_EMISE).build();
    }

    @Bean
    public Queue queueDpdStatut() {
        return QueueBuilder.durable(DpdBus.QUEUE_STATUT).build();
    }

    @Bean
    public Queue queueDpdMaj() {
        return QueueBuilder.durable(DpdBus.QUEUE_MAJ).build();
    }

    @Bean
    public Binding bindingDpdEmise(Queue queueDpdEmise, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueDpdEmise).to(evenementsExchange).with(DpdBus.RK_DEMANDE_EMISE);
    }

    @Bean
    public Binding bindingDpdStatut(Queue queueDpdStatut, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueDpdStatut).to(evenementsExchange).with(DpdBus.RK_STATUT);
    }

    @Bean
    public Binding bindingDpdMaj(Queue queueDpdMaj, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueDpdMaj).to(evenementsExchange).with(DpdBus.RK_MAJ);
    }
}
