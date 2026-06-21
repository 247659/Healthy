package healthmonitor.notifications.sevice;

import healthmonitor.notifications.communication.AlertNotifier;
import healthmonitor.notifications.communication.MedicalStaffClient;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;
import healthmonitor.notifications.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final MedicalStaffClient medicalStaffClient;
    private final AlertNotifier alertNotifier;

    @Override
    @Transactional
    public void processAlert(AlertEventMessage alertDto) {
        log.info("Receiver alert for user with ID: {}", alertDto.getPatientId());
        Alert alert = Alert.builder()
                .patientId(alertDto.getPatientId())
                .riskScore(alertDto.getRiskScore())
                .severity(alertDto.getSeverity())
                .message(alertDto.getMessage())
                .details(alertDto.getDetails())
                .recommendation(alertDto.getRecommendation())
                .forecastNote(alertDto.getForecastNote())
                .method(alertDto.getMethod())
                .timestamp(alertDto.getTimestamp())
                .build();

        Alert save = alertRepository.save(alert);
        log.info("Alert saved in database with ID: {}", save.getAlertId());
        try {
            sendNotificationToDoctors(save);
        } catch (Exception e) {
            log.error("Error while saving alert: {}", e.getMessage());
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

        alertNotifier.notifyDoctors(alert, doctorIds);
    }

    private AlertDto mapToDto(Alert alert) {
        return AlertDto.builder()
                .alertId(alert.getAlertId())
                .patientId(alert.getPatientId())
                .riskScore(alert.getRiskScore())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .details(alert.getDetails())
                .timestamp(alert.getTimestamp())
                .isRead(alert.isRead())
                .build();
    }
}
