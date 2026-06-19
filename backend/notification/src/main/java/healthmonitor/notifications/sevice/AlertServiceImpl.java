package healthmonitor.notifications.sevice;

import healthmonitor.notifications.communication.MedicalStaffClient;
import healthmonitor.notifications.config.RabbitMQConfig;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;
import healthmonitor.notifications.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final MedicalStaffClient medicalStaffClient;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional
    public void processAlert(AlertEventMessage alertDto) {
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

            sendNotificationToDoctors(save);
        } catch (WebClientResponseException e) {
            log.error("HTTP error from MedicalStaffService: {}", e.getStatusCode(), e);

        } catch (WebClientRequestException e) {
            log.error("Connection error to MedicalStaffService", e);

        } catch (Exception e) {
            log.error("Error while saving alert: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<AlertDto> getNotificationsForPatient(String patientId) {
        List<Alert> alerts = alertRepository.findByPatientIdAndIsReadFalseOrderByTimestampDesc(patientId);
        return alerts.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public List<AlertDto> getAllNotifications(String patientId) {
        List<Alert> alerts = alertRepository.findByPatientId(patientId);
        return alerts.stream()
                .map(this::mapToDto)
                .toList();
    }

    private void sendNotificationToDoctors(Alert alert) {
        List<String> doctorIds = medicalStaffClient.getDoctorIdsForPatient(alert.getPatientId());

        if (doctorIds == null || doctorIds.isEmpty()) {
            log.warn("No doctors assigned for patient {}.", alert.getPatientId());
            return;
        }

        for (String doctorId : doctorIds) {
            // Spring pod maską wysyła to na adres: /user/{doctorId}/queue/alerts
            messagingTemplate.convertAndSendToUser(
                    doctorId,
                    "/queue/alerts",
                    alert
            );
            log.info("WebSocket notification sent to doctor: {}", doctorId);
        }
    }

    private AlertDto mapToDto(Alert alert) {
        return AlertDto.builder()
                .alertId(alert.getAlertId())
                .patientId(alert.getPatientId())
                .riskScore(alert.getRiskScore())
                .message(alert.getMessage())
                .timestamp(alert.getTimestamp())
                .isRead(alert.isRead())
                .build();
    }
}
