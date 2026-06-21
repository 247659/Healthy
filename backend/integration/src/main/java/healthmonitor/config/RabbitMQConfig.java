package healthmonitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
}
