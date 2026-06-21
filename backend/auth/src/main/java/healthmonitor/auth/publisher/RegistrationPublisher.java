package healthmonitor.auth.publisher;

import healthmonitor.auth.config.RabbitMQConfig;
import healthmonitor.auth.model.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistration(String role, UserRegisteredEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AUTH_EXCHANGE,
                "user.registered." + role,
                event)
        ;
    }
}
