package healthmonitor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "iot.vitals.exchange";
    public static final String BATCH_QUEUE = "vitals.batch";
    public static final String ROUTING_KEY = "vitals.batch.incoming";
    public static final String VITALS_ROUTING_KEY = "vitals.incoming";
    public static final String DLQ_EXCHANGE = "dlq.exchange";
    public static final String ML_QUEUE_NAME = "vitals.ml.queue";
    public static final String DLQ_QUEUE_ML = "vitals.ml.dlq";

    @Bean
    public TopicExchange vitalsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Queue batchQueue() {
        return new Queue(BATCH_QUEUE, true);
    }

    @Bean
    public Binding batchBinding(Queue batchQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(batchQueue).to(vitalsExchange).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Queue mlQueue() {
        return QueueBuilder.durable(ML_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ML_QUEUE_NAME)
                .build();
    }

    @Bean
    public Queue dlqMlQueue() {
        return QueueBuilder.durable(DLQ_QUEUE_ML).build();
    }

    @Bean
    public Binding mlBinding(Queue mlQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(mlQueue).to(vitalsExchange).with(VITALS_ROUTING_KEY);
    }

    @Bean
    public Binding dlqMlBinding(Queue dlqMlQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqMlQueue).to(dlqExchange).with(ML_QUEUE_NAME);
    }
}
