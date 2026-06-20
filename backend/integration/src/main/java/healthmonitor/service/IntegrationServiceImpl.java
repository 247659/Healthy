package healthmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.config.RabbitMQConfig;
import healthmonitor.dto.VitalSignsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IntegrationService {

    private final ObjectMapper objectMapper;
    private static final int BATCH_SIZE = 2000;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void processBatchMeasurements(MultipartFile file) {
        log.info("Rozpoczęto przetwarzanie pliku wsadowego: {}", file.getOriginalFilename());

        List<VitalSignsDto> batch = new ArrayList<>(BATCH_SIZE);
        int totalProcessed = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                VitalSignsDto dto = objectMapper.readValue(line, VitalSignsDto.class);
                batch.add(dto);

                if (batch.size() >= BATCH_SIZE) {
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, new ArrayList<>(batch));
                    totalProcessed += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, new ArrayList<>(batch));
                totalProcessed += batch.size();
            }

            log.info("Zakończono sukcesem. Przekazano do RabbitMQ łącznie {} pomiarów w paczkach po {}.", totalProcessed, BATCH_SIZE);

        } catch (Exception e) {
            log.error("Krytyczny błąd podczas przetwarzania pliku wsadowego", e);
            throw new RuntimeException("Błąd podczas przetwarzania pliku wsadowego", e);
        }
    }
}