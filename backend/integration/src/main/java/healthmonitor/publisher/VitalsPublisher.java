package healthmonitor.publisher;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.dto.VitalSignsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VitalsPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicVitals(VitalSignsDto dto) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.VITALS_ROUTING_KEY,
                dto
        );
    }
}
