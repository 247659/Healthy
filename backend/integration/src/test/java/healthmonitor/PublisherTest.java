package healthmonitor;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.dto.VitalSignsDto;
import healthmonitor.publisher.BatchPublisher;
import healthmonitor.publisher.VitalsPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PublisherTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BatchPublisher batchPublisher;

    @InjectMocks
    private VitalsPublisher vitalsPublisher;

    @Test
    void shouldPublishBatchToRabbitMQ() {
        List<VitalSignsDto> batch = List.of(new VitalSignsDto());

        batchPublisher.publishBatch(batch);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                batch
        );
    }

    @Test
    void shouldPublishVitalsToRabbitMq() {
        VitalSignsDto vitalSignsDto = new VitalSignsDto();

        vitalsPublisher.publicVitals(vitalSignsDto);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.VITALS_ROUTING_KEY,
                vitalSignsDto
        );
    }
}
