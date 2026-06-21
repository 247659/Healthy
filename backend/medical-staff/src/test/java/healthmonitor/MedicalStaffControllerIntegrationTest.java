package healthmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.medicalStaff.controller.MedicalStaffController;
import healthmonitor.medicalStaff.payload.request.MedicalStaffCreateRequest;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.request.SpecializationRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffEssentialResponse;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.service.MedicalStaffService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalStaffController.class)
class MedicalStaffControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MedicalStaffService medicalStaffService;

    @Test
    void shouldGetAll() throws Exception {
        List<MedicalStaffResponse> responses = List.of(
                new MedicalStaffResponse("doc-1", "Jan", "Kowalski", "123456789", "1234567", List.of())
        );
        when(medicalStaffService.getAll()).thenReturn(responses);

        mockMvc.perform(get("/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value("doc-1"))
                .andExpect(jsonPath("$[0].firstName").value("Jan"));
    }

    @Test
    void shouldGetById() throws Exception {
        MedicalStaffResponse response = new MedicalStaffResponse("doc-1", "Jan", "Kowalski", "123456789", "1234567", List.of());
        when(medicalStaffService.getById("doc-1")).thenReturn(response);

        mockMvc.perform(get("/staff/doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doc-1"));
    }

    @Test
    void shouldSave() throws Exception {
        MedicalStaffCreateRequest request = createValidCreateRequest();
        MedicalStaffResponse response = new MedicalStaffResponse("doc-2", "Anna", "Nowak", "987654321", "7654321", List.of());

        when(medicalStaffService.save(any(MedicalStaffCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("doc-2"))
                .andExpect(jsonPath("$.firstName").value("Anna"));
    }

    @Test
    void shouldDelete() throws Exception {
        mockMvc.perform(delete("/staff/doc-1"))
                .andExpect(status().isNoContent());

        verify(medicalStaffService, times(1)).delete("doc-1");
    }

    @Test
    void shouldUpdate() throws Exception {
        MedicalStaffRequest request = new MedicalStaffRequest("Anna", "Nowak", "987654321", "7654321", List.of(), "pass123");
        MedicalStaffResponse response = new MedicalStaffResponse("doc-1", "Anna", "Nowak", "987654321", "7654321", List.of());

        when(medicalStaffService.update(eq("doc-1"), any(MedicalStaffRequest.class))).thenReturn(response);

        mockMvc.perform(put("/staff/doc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"));
    }

    @Test
    void shouldAssignPatient() throws Exception {
        mockMvc.perform(post("/staff/doc-1/assign/pat-1"))
                .andExpect(status().isNoContent());

        verify(medicalStaffService, times(1)).assignPatient("doc-1", "pat-1");
    }

    @Test
    void shouldGetAllPatientIds() throws Exception {
        when(medicalStaffService.getPatientsIds("doc-1")).thenReturn(List.of("pat-1", "pat-2"));

        mockMvc.perform(get("/staff/doc-1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0]").value("pat-1"))
                .andExpect(jsonPath("$[1]").value("pat-2"));
    }

    @Test
    void shouldGetDoctorsForPatient() throws Exception {
        when(medicalStaffService.getDoctorsIdsByPatientId("pat-1")).thenReturn(List.of("doc-1", "doc-2"));

        mockMvc.perform(get("/staff/patients/pat-1/doctors-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0]").value("doc-1"));
    }

    @Test
    void shouldGetAllDoctorsEssentialData() throws Exception {
        MedicalStaffEssentialResponse essentialResponse = new MedicalStaffEssentialResponse("doc-1", "Jan", "Kowalski", List.of());
        when(medicalStaffService.getAllDoctorsEssentialData()).thenReturn(List.of(essentialResponse));

        mockMvc.perform(get("/staff/essential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value("doc-1"));
    }

    @Test
    void shouldGetDoctorEssentialDataById() throws Exception {
        MedicalStaffEssentialResponse essentialResponse = new MedicalStaffEssentialResponse("doc-1", "Jan", "Kowalski", List.of());
        when(medicalStaffService.getDoctorEssentialDataById("doc-1")).thenReturn(essentialResponse);

        mockMvc.perform(get("/staff/doc-1/essential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doc-1"));
    }

    @Test
    void shouldGetDoctorsAssignedToPatient() throws Exception {
        MedicalStaffEssentialResponse essentialResponse = new MedicalStaffEssentialResponse("doc-1", "Jan", "Kowalski", List.of());
        when(medicalStaffService.getDoctorsAssignedToPatient("pat-1")).thenReturn(List.of(essentialResponse));

        mockMvc.perform(get("/staff/patients/pat-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value("doc-1"));
    }

    @Test
    void shouldUnassignPatient() throws Exception {
        mockMvc.perform(delete("/staff/doc-1/unassign/pat-1"))
                .andExpect(status().isNoContent());

        verify(medicalStaffService, times(1)).unassignPatient("doc-1", "pat-1");
    }

    private MedicalStaffCreateRequest createValidCreateRequest() {
        SpecializationRequest spec = new SpecializationRequest("Cardiology", LocalDate.of(2015, 5, 20), "1234567");
        return new MedicalStaffCreateRequest(
                "doc-2",
                "Anna",
                "Nowak",
                "987654321",
                "7654321",
                List.of(spec),
                "securePassword123"
        );
    }
}