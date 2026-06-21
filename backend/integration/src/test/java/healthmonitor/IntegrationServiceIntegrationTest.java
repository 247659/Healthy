package healthmonitor;

import healthmonitor.dto.VitalSignsDto;
import healthmonitor.publisher.BatchPublisher;
import healthmonitor.publisher.VitalsPublisher;
import healthmonitor.service.IntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
class IntegrationServiceIntegrationTest {

    @MockitoBean
    private BatchPublisher batchPublisher;

    @MockitoBean
    private VitalsPublisher vitalsPublisher;

    @Autowired
    private IntegrationService integrationService;

    @Test
    void shouldProcessFileAndPublishBatches() {
        String jsonLine = "{\"patientId\":\"test\"}\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "text/plain",
                jsonLine.repeat(2005).getBytes(StandardCharsets.UTF_8));

        integrationService.processBatchMeasurements(file);

        verify(batchPublisher, times(2)).publishBatch(anyList());
        verify(batchPublisher).publishBatch(argThat(list -> list.size() == 2000));
        verify(batchPublisher).publishBatch(argThat(list -> list.size() == 5));
    }

    @Test
    void shouldReceiveSingleVitals() {
        VitalSignsDto dto = new VitalSignsDto();
        dto.setPatientId("pat-123");

        integrationService.receiveVitals(dto);

        verify(vitalsPublisher, times(1)).publicVitals(dto);
    }
}