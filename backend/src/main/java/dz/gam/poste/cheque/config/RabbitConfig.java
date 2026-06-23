package dz.gam.poste.cheque.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologie du bus pour le suivi des chèques (ADR 0004) : un topic exchange, deux files.
 *
 * <p>La sérialisation JSON utilise l'{@link ObjectMapper} de Spring Boot (modules
 * java.time inclus) afin que {@code Instant}/{@code LocalDate} des messages soient
 * correctement (dé)sérialisés. Spring Boot branche automatiquement ce convertisseur
 * sur le {@code RabbitTemplate} et sur la fabrique de listeners.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange evenementsExchange() {
        return new TopicExchange(BusCheque.EXCHANGE, true, false);
    }

    @Bean
    public Queue queueChequeEmis() {
        return QueueBuilder.durable(BusCheque.QUEUE_CHEQUE_EMIS).build();
    }

    @Bean
    public Queue queueWriteBack() {
        return QueueBuilder.durable(BusCheque.QUEUE_WRITE_BACK).build();
    }

    @Bean
    public Binding bindingChequeEmis(Queue queueChequeEmis, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueChequeEmis).to(evenementsExchange).with(BusCheque.RK_CHEQUE_EMIS);
    }

    @Bean
    public Binding bindingWriteBack(Queue queueWriteBack, TopicExchange evenementsExchange) {
        return BindingBuilder.bind(queueWriteBack).to(evenementsExchange).with(BusCheque.RK_CHEQUE_STATUT_FINALISE);
    }

    @Bean
    public MessageConverter messageConverterJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
