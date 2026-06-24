package healthmonitor.service;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.model.event.UserRegisteredEvent;
import healthmonitor.model.Patient;
import healthmonitor.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientMessageListener {

    private final PatientRepository patientRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handlePatientRegistrationEvent(UserRegisteredEvent event) {
        log.info("Registration event received for email: {}", event.getEmail());
        try {
            Patient newPatient = new Patient();
            newPatient.setId(event.getKeycloakUserId());
            newPatient.setFirstName(event.getFirstName());
            newPatient.setLastName(event.getLastName());
            newPatient.setEmail(event.getEmail());
            newPatient.setCreatedAt(LocalDateTime.now());
            patientRepository.save(newPatient);
            log.info("Patient record was created with external ID.: {}", event.getKeycloakUserId());
        } catch (IllegalArgumentException e) {
            log.error("Nieprawidłowe dane pacjenta. Wysyłam do DLQ: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Błąd walidacji danych pacjenta", e);
        }
    }
}