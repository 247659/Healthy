package healthmonitor.vitals.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "vitals.queue";
    public static final String BATCH_QUEUE_NAME = "vitals.batch";
    public static final String THRESHOLD_QUEUE = "threshold.queue";

    public static final String EXCHANGE_NAME = "iot.vitals.exchange";

    public static final String ROUTING_KEY = "vitals.incoming";
    public static final String BATCH_ROUTING_KEY = "vitals.batch.incoming";
    public static final String ROUTING_KEY_THRESHOLD = "vitals.threshold.created";

    public static final String DLQ_EXCHANGE = "dlq.exchange";
    public static final String DLQ_QUEUE_VITALS = "vitals.dlq";
    public static final String DLQ_QUEUE_BATCH = "vitals.batch.dlq";
    public static final String DLQ_QUEUE_THRESHOLD = "threshold.dlq";

    @Bean
    public TopicExchange vitalsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Queue vitalsQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_NAME)
                .build();
    }

    @Bean
    public Queue thresholdQueue() {
        return QueueBuilder.durable(THRESHOLD_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", THRESHOLD_QUEUE)
                .build();
    }

    @Bean
    public Queue batchQueue() {
        return QueueBuilder.durable(BATCH_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BATCH_QUEUE_NAME)
                .build();
    }

    @Bean
    public Queue dlqVitalsQueue() {
        return QueueBuilder.durable(DLQ_QUEUE_VITALS).build();
    }

    @Bean
    public Queue dlqBatchQueue() {
        return QueueBuilder.durable(DLQ_QUEUE_BATCH).build();
    }

    @Bean
    public Queue dlqThresholdQueue() {
        return QueueBuilder.durable(DLQ_QUEUE_THRESHOLD).build();
    }

    @Bean
    public Binding vitalsBinding(Queue vitalsQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(vitalsQueue).to(vitalsExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding thresholdBinding(Queue thresholdQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(thresholdQueue).to(vitalsExchange).with(ROUTING_KEY_THRESHOLD);
    }

    @Bean
    public Binding batchBinding(Queue batchQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(batchQueue).to(vitalsExchange).with(BATCH_ROUTING_KEY);
    }

    @Bean
    public Binding dlqVitalsBinding(Queue dlqVitalsQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqVitalsQueue).to(dlqExchange).with(QUEUE_NAME);
    }

    @Bean
    public Binding dlqBatchBinding(Queue dlqBatchQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqBatchQueue).to(dlqExchange).with(BATCH_QUEUE_NAME);
    }

    @Bean
    public Binding dlqThresholdBinding(Queue dlqThresholdQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqThresholdQueue).to(dlqExchange).with(THRESHOLD_QUEUE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}