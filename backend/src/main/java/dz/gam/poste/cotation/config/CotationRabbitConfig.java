package dz.gam.poste.cotation.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologie du bus pour la cotation, sur l'exchange topic commun du poste (injecté). Deux
 * files : une consommée par le mock central, une par le poste (retours de statut).
 */
@Configuration
public class CotationRabbitConfig {

    @Bean
    public Queue queueCotationEmise() {
        return QueueBuilder.durable(CotationBus.QUEUE_DEMANDE_EMISE).build();
    }

    @Bean
    public Queue queueCotationStatut() {
        return QueueBuilder.durable(CotationBus.QUEUE_COTATION_STATUT).build();
    }

    @Bean
    public Binding bindingCotationEmise(Queue queueCotationEmise, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueCotationEmise).to(evenementsExchange).with(CotationBus.RK_DEMANDE_EMISE);
    }

    @Bean
    public Binding bindingCotationStatut(Queue queueCotationStatut, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueCotationStatut).to(evenementsExchange).with(CotationBus.RK_COTATION_STATUT);
    }
}
