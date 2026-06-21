package healthmonitor;

import healthmonitor.controller.IntegrationController;
import healthmonitor.dto.VitalSignsDto;
import healthmonitor.service.IntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IntegrationController.class)
class IntegrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntegrationService integrationService;

    @Test
    void shouldReceiveSingleVitals() throws Exception {
        String json = "{\"patientId\":\"pat-123\", \"timestamp\":\"2026-06-21T18:00:00Z\", \"measurements\":{\"heartRate\":80, \"temperature\":36.6, \"spO2\":98, \"bloodPressure\":{\"systolic\":120, \"diastolic\":80}}}";

        mockMvc.perform(post("/api/v1/integration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());

        verify(integrationService).receiveVitals(any(VitalSignsDto.class));
    }

    @Test
    void shouldReceiveBatchFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "text/plain", "data".getBytes());

        mockMvc.perform(multipart("/api/v1/integration/batch")
                        .file(file))
                .andExpect(status().isNoContent());

        verify(integrationService).processBatchMeasurements(any());
    }
}