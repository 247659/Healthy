package healthmonitor.notifications.sevice;

import healthmonitor.notifications.config.RabbitMQConfig;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;

    @Override
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional
    public void processAlert(AlertDto alertDto) {
        log.info("Receiver alert for user with ID: {}", alertDto.getPatientId());

        try {
            Alert alert = Alert.builder()
                    .patientId(alertDto.getPatientId())
                    .riskScore(alertDto.getRiskScore())
                    .message(alertDto.getMessage())
                    .timestamp(alertDto.getTimestamp())
                    .build();

            Alert save = alertRepository.save(alert);
            log.info("Alert saved in database with ID: {}", save.getAlertId());
        } catch (Exception e) {
            log.error("Error while saving alert: {}", e.getMessage());

            throw e;
        }

    }
}
