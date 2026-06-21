package healthmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.dto.VitalSignsDto;
import healthmonitor.publisher.BatchPublisher;
import healthmonitor.publisher.VitalsPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BatchPublisher batchPublisher;
    private final VitalsPublisher vitalsPublisher;

    @Override
    public void receiveVitals(VitalSignsDto dto) {
        log.info("Measurements received for patient: {}", dto.getPatientId());
        vitalsPublisher.publicVitals(dto);
    }

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
                    batchPublisher.publishBatch(new ArrayList<>(batch));
                    totalProcessed += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                batchPublisher.publishBatch(batch);
                totalProcessed += batch.size();
            }

            log.info("Zakończono sukcesem. Przekazano do RabbitMQ łącznie {} pomiarów w paczkach po {}.", totalProcessed, BATCH_SIZE);

        } catch (Exception e) {
            log.error("Krytyczny błąd podczas przetwarzania pliku wsadowego", e);
            throw new RuntimeException("Błąd podczas przetwarzania pliku wsadowego", e);
        }
    }
}