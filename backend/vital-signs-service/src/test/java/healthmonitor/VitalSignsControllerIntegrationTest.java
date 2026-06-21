package healthmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.vitals.controller.VitalSignsController;
import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.dto.VitalThresholdDto;
import healthmonitor.vitals.service.VitalsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VitalSignsController.class)
class VitalSignsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VitalsService vitalsService;

    @Test
    void shouldGetHistory() throws Exception {
        String patientId = "pat-123";
        when(vitalsService.getPatientHistory(eq(patientId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new VitalSignsDto()));

        mockMvc.perform(get("/vital-signs/patient/{patientId}", patientId)
                        .param("from", Instant.now().minusSeconds(3600).toString())
                        .param("to", Instant.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void shouldGetThresholdByPatientId() throws Exception {
        String patientId = "pat-123";
        VitalThresholdDto dto = new VitalThresholdDto(80, 70, 95, 120, BigDecimal.valueOf(36.6));
        when(vitalsService.getByPatientId(patientId)).thenReturn(dto);

        mockMvc.perform(get("/vital-signs/threshold/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systolicBp").value(120))
                .andExpect(jsonPath("$.heartRate").value(70));
    }

    @Test
    void shouldUpdateThreshold() throws Exception {
        String patientId = "pat-123";
        VitalThresholdDto request = new VitalThresholdDto(90, 75, 98, 130, BigDecimal.valueOf(36.7));
        when(vitalsService.update(eq(patientId), any(VitalThresholdDto.class))).thenReturn(request);

        mockMvc.perform(put("/vital-signs/threshold/{patientId}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systolicBp").value(130));
    }
}