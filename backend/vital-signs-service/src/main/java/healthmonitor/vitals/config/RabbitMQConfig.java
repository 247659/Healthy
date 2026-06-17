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