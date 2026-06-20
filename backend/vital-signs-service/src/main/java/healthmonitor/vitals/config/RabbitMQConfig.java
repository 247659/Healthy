package healthmonitor.vitals.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "vitals.queue";
    public static final String EXCHANGE_NAME = "iot.vitals.exchange";
    public static final String ROUTING_KEY = "vitals.incoming";
    public static final String BATCH_QUEUE_NAME = "vitals.batch";
    public static final String BATCH_ROUTING_KEY = "vitals.batch.incoming";
    public static final String THRESHOLD_QUEUE = "threshold.queue";
    public static final String ROUTING_KEY_THRESHOLD = "vitals.threshold.created";

    @Bean
    public Queue vitalsQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public TopicExchange vitalsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding vitalsBinding(Queue vitalsQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(vitalsQueue).to(vitalsExchange).with(ROUTING_KEY);
    }

    @Bean
    public Queue thresholdQueue() {
        return new Queue(THRESHOLD_QUEUE, true);
    }

    @Bean
    public Binding thresholdBinding(Queue thresholdQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(thresholdQueue).to(vitalsExchange).with(ROUTING_KEY_THRESHOLD);
    }

    @Bean
    public Queue batchQueue() {
        return new Queue(BATCH_QUEUE_NAME, true);
    }

    @Bean
    public Binding batchBinding(Queue batchQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(batchQueue).to(vitalsExchange).with(BATCH_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}