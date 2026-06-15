package healthmonitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String QUEUE_NAME = "vitals.storage.queue";
    public static final String EXCHANGE_NAME = "iot.vitals.exchange";

    @Bean
    public Queue vitalsQueue() { return new Queue(QUEUE_NAME, true); }

    @Bean
    public TopicExchange vitalsExchange() { return new TopicExchange(EXCHANGE_NAME); }

    @Bean
    public Binding binding(Queue vitalsQueue, TopicExchange vitalsExchange) {
        return BindingBuilder.bind(vitalsQueue).to(vitalsExchange).with("vitals.incoming");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
