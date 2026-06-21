package healthmonitor.publisher;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.dto.VitalSignsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishBatch(List<VitalSignsDto> batch) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                batch
        );
    }

}
