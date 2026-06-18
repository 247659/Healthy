package healthmonitor.notifications.config;

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

    public static final String QUEUE_NAME = "notifications.queue";
    public static final String EXCHANGE_NAME = "notifications.exchange";
    public static final String ROUTING_KEY = "notifications.incoming";

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
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}