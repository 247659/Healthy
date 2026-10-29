package healthmonitor.notifications.sevice;

import healthmonitor.notifications.communication.AlertNotifier;
import healthmonitor.notifications.communication.MedicalStaffClient;
import healthmonitor.notifications.exception.MarkingAsReadException;
import healthmonitor.notifications.model.Alert;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.AlertEventMessage;
import healthmonitor.notifications.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public void processAlert(AlertEventMessage alertDto) {
        log.info("Receiver alert for user with ID: {}", alertDto.getPatientId());
        try {
            processForPatient(mapToEntity(alertDto));
            processForDoctors(mapToEntity(alertDto));
        } catch (Exception e) {
            log.error("Error while saving alert: {}", e.getMessage());
        }
    }

    @Override
    public List<AlertDto> getNotificationsForPatient(String patientId, String userId) {
        List<Alert> alerts = alertRepository.findByPatientIdAndDestinationAndIsReadFalseOrderByTimestampDesc(patientId, userId);
        return alerts.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public Page<AlertDto> getAllNotifications(String patientId, String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<Alert> alerts = alertRepository.findByPatientIdAndDestination(patientId, userId, pageable);
        return alerts.map(this::mapToDto);
    }

    @Override
    @Transactional
    public void markAsRead(List<String> ids, String userId) {
        try {
            List<Alert> alerts = alertRepository.findAllByAlertIdAndDestination(ids, userId);
            if (alerts.isEmpty()) {
                log.warn("No alerts found for the provided IDs and user ID: {}", userId);
                throw new MarkingAsReadException("No alerts found for the provided IDs and user ID: " + userId);
            }
            alerts.forEach(alert -> alert.setRead(true));
            alertRepository.saveAll(alerts);
        } catch (Exception e) {
            log.error("Error while marking alerts as read: {}", e.getMessage());
            throw new MarkingAsReadException("Error while marking alerts as read: " + e.getMessage());
        }
    }

    private void processForPatient(Alert alert) {
        try {
            alert.setDestination(alert.getPatientId());
            Alert save = alertRepository.save(alert);
            log.info("Alert saved in database with ID: {}, for patient with ID: {}", save.getAlertId(), save.getPatientId());
            //TODO KOD DO WYSŁANIA NOTYFIKACJI DO PACJENTA
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void processForDoctors(Alert alert) {
        List<String> doctorIds = medicalStaffClient.getDoctorIdsForPatient(alert.getPatientId());

        if (doctorIds == null || doctorIds.isEmpty()) {
            log.warn("No doctors assigned for patient {}.", alert.getPatientId());
            return;
        }

        for (String doctorId : doctorIds) {
            alert.setDestination(doctorId);
            try {
                Alert save = alertRepository.save(alert);
                log.info("Alert saved in database with ID: {}, for doctor with ID: {}", save.getAlertId(), save.getDestination());

                alertNotifier.notifyDoctor(mapToDto(alert), doctorId);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
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

    private Alert mapToEntity(AlertEventMessage alertDto) {
        return Alert.builder()
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
    }
}
