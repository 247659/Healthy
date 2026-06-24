package healthmonitor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "patient.registration.queue";
    public static final String AUTH_EXCHANGE = "auth.exchange";

    public static final String QUEUE_NAME_VITALS = "vitals.queue";
    public static final String IOT_EXCHANGE = "iot.vitals.exchange";
    public static final String VITALS_ROUTING_KEY = "vitals.threshold.created";

    public static final String DLQ_EXCHANGE = "dlq.exchange";
    public static final String DLQ_QUEUE_REGISTRATION = "patient.registration.dlq";
    public static final String DLQ_QUEUE_VITALS = "vitals.dlq";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    @Bean
    public TopicExchange iotExchange() {
        return new TopicExchange(IOT_EXCHANGE);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Queue patientRegistrationQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_NAME)
                .build();
    }

    @Bean
    public Queue patientThresholdQueue() {
        return QueueBuilder.durable(QUEUE_NAME_VITALS)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_NAME_VITALS)
                .build();
    }

    @Bean
    public Queue dlqRegistrationQueue() {
        return QueueBuilder.durable(DLQ_QUEUE_REGISTRATION).build();
    }

    @Bean
    public Queue dlqVitalsQueue() {
        return QueueBuilder.durable(DLQ_QUEUE_VITALS).build();
    }

    @Bean
    public Binding binding(Queue patientRegistrationQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(patientRegistrationQueue).to(authExchange).with("user.registered.patient");
    }

    @Bean
    public Binding iotBinding(Queue patientThresholdQueue, TopicExchange iotExchange) {
        return BindingBuilder.bind(patientThresholdQueue).to(iotExchange).with(VITALS_ROUTING_KEY);
    }

    @Bean
    public Binding dlqRegistrationBinding(Queue dlqRegistrationQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqRegistrationQueue).to(dlqExchange).with(QUEUE_NAME);
    }

    @Bean
    public Binding dlqVitalsBinding(Queue dlqVitalsQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqVitalsQueue).to(dlqExchange).with(QUEUE_NAME_VITALS);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}