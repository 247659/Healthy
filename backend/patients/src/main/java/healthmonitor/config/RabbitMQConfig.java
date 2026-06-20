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

    public static final String QUEUE_NAME = "patient.registration.queue";
    public static final String AUTH_EXCHANGE = "auth.exchange";
    public static final String IOT_EXCHANGE = "iot.vitals.exchange";
    public static final String QUEUE_NAME_VITALS = "vitals.queue";
    public static final String VITALS_ROUTING_KEY = "vitals.threshold.created";


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
    public TopicExchange iotExchange() {
        return new TopicExchange(IOT_EXCHANGE);
    }

    @Bean
    public Queue patientThresholdQueue() {
        return new Queue(QUEUE_NAME_VITALS, true);
    }

    @Bean
    public Binding iotBinding(Queue patientThresholdQueue, TopicExchange iotExchange) {
        return BindingBuilder.bind(patientThresholdQueue).to(iotExchange).with(VITALS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
