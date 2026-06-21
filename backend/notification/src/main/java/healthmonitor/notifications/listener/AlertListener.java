package healthmonitor.notifications.listener;

import healthmonitor.notifications.config.RabbitMQConfig;
import healthmonitor.notifications.model.AlertEventMessage;
import healthmonitor.notifications.sevice.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertListener {

    private final AlertService alertService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveAlert(AlertEventMessage message) {
        log.info("Received message from RabbitMQ for patient: {}", message.getPatientId());
        try {
            alertService.processAlert(message);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ message", e);
        }
    }
}
