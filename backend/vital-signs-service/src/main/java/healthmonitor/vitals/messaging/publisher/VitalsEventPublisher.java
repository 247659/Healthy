package healthmonitor.vitals.messaging.publisher;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.vitals.messaging.event.VitalsRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VitalsEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishVitalsRegisteredEvent(VitalsRegisteredEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}