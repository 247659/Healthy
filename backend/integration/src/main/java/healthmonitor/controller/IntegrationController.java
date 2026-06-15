package healthmonitor.controller;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.dto.VitalSignsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
@Slf4j
public class IntegrationController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping
    public ResponseEntity<Void> receiveVitals(@Valid @RequestBody VitalSignsDto dto) {
        log.info("Measurements received for patient: {}", dto.getPatientId());

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "vitals.incoming", dto);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
