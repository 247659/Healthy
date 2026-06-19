package healthmonitor.service;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.model.event.PatientThresholdEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VitalThresholdEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishThresholdCreate(PatientThresholdEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.IOT_EXCHANGE,
                RabbitMQConfig.VITALS_ROUTING_KEY,
                event
        );
    }
}
