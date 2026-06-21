package healthmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.controller.PatientController;
import healthmonitor.exception.exceptions.DuplicateResourceException;
import healthmonitor.exception.exceptions.PatientNotFoundException;
import healthmonitor.model.dto.PatientDto;
import healthmonitor.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
public class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    private PatientDto createValidDto() {
        PatientDto dto = new PatientDto();
        dto.setFirstName("Jan");
        dto.setLastName("Kowalski");
        dto.setEmail("jan@example.com");
        dto.setPesel("12345678901");
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dto.setPhoneNumber("123456789");
        dto.setAddress("Testowa 1");
        return dto;
    }

    @Test
    void shouldGetAllPatients() throws Exception {
        PatientDto dto = createValidDto();
        when(patientService.save(any(PatientDto.class))).thenReturn(dto);

        mockMvc.perform(get("/patients/allPatients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldSavePatient() throws Exception {
        PatientDto dto = createValidDto();
        when(patientService.save(any(PatientDto.class))).thenReturn(dto);

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"));
    }

    @Test
    void shouldGetPatientById() throws Exception {
        PatientDto dto = createValidDto();
        when(patientService.getPatientById("123")).thenReturn(dto);

        mockMvc.perform(get("/patients/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"));
    }

    @Test
    void shouldUpdatePatient() throws Exception {
        PatientDto dto = createValidDto();
        when(patientService.updatePatient(eq("123"), any(PatientDto.class))).thenReturn(dto);

        mockMvc.perform(put("/patients/update/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenPatientNotFound() throws Exception {
        when(patientService.getPatientById("999"))
                .thenThrow(new PatientNotFoundException("Not found"));

        mockMvc.perform(get("/patients/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenSaveInvalidData() throws Exception {
        PatientDto invalidDto = createValidDto();
        invalidDto.setPesel("invalid");

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUpdateInvalidData() throws Exception {
        PatientDto invalidDto = createValidDto();
        invalidDto.setFirstName("");

        mockMvc.perform(put("/patients/update/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenDuplicateResourceFound() throws Exception {
        when(patientService.save(any(PatientDto.class)))
                .thenThrow(new DuplicateResourceException("Patient with this PESEL already exists"));

        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidDto())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Patient with this PESEL already exists"))
                .andExpect(jsonPath("$.status").value(409));
    }
}