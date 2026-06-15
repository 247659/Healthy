package healthmonitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "patient.registration.queue";
    public static final String AUTH_EXCHANGE = "auth.exchange";

    @Bean
    public Queue patientRegistrationQueue() {
        return new Queue(QUEUE_NAME, true); // true = kolejka przetrwa restart RabbitMQ
    }

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    // Wiążemy kolejkę z Exchange używając klucza routingu
    @Bean
    public Binding binding(Queue patientRegistrationQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(patientRegistrationQueue).to(authExchange).with("user.registered.patient");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
