package healthmonitor;

import healthmonitor.client.AuthClient;
import healthmonitor.client.PatientClient;
import healthmonitor.medicalStaff.payload.request.MedicalStaffCreateRequest;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.request.SpecializationRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffEssentialResponse;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.repository.MedicalStaffRepository;
import healthmonitor.medicalStaff.service.MedicalStaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
public class MedicalStaffServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MedicalStaffService medicalStaffService;

    @Autowired
    private MedicalStaffRepository medicalStaffRepository;

    @MockitoBean
    private PatientClient patientClient;

    @MockitoBean
    private AuthClient authClient;

    @BeforeEach
    void setUp() {
        medicalStaffRepository.deleteAll();
    }

    @Test
    void shouldSaveNewMedicalStaff() {
        MedicalStaffCreateRequest request = createSampleCreateRequest("doc-1");

        MedicalStaffResponse response = medicalStaffService.save(request);

        assertNotNull(response);
        assertEquals("doc-1", response.id());
        assertEquals("Jan", response.firstName());

        assertTrue(medicalStaffRepository.findById("doc-1").isPresent());

        verify(authClient, times(1)).updatePassword("doc-1", "securePassword123");
    }

    @Test
    void shouldUpdateExistingMedicalStaff() {
        medicalStaffService.save(createSampleCreateRequest("doc-2"));

        MedicalStaffRequest updateRequest = new MedicalStaffRequest(
                "Adam",
                "Nowak",
                "987654321",
                "9876543",
                List.of(),
                "newPassword123"
        );

        MedicalStaffResponse response = medicalStaffService.update("doc-2", updateRequest);

        assertEquals("Adam", response.firstName());
        assertEquals("Nowak", response.lastName());

        verify(authClient, times(1)).updatePassword("doc-2", "newPassword123");
    }

    @Test
    void shouldDeleteMedicalStaff() {
        medicalStaffService.save(createSampleCreateRequest("doc-3"));
        assertTrue(medicalStaffRepository.findById("doc-3").isPresent());

        medicalStaffService.delete("doc-3");

        assertFalse(medicalStaffRepository.findById("doc-3").isPresent());
    }

    @Test
    void shouldAssignPatientToMedicalStaff() {
        String doctorId = "doc-4";
        String patientId = "patient-100";
        medicalStaffService.save(createSampleCreateRequest(doctorId));

        when(patientClient.getPatient(patientId)).thenReturn(null);

        medicalStaffService.assignPatient(doctorId, patientId);

        // then
        List<String> assignedPatients = medicalStaffService.getPatientsIds(doctorId);
        assertTrue(assignedPatients.contains(patientId));
        assertEquals(1, assignedPatients.size());
    }

    @Test
    void shouldThrowExceptionWhenAssigningSamePatientTwice() {
        String doctorId = "doc-5";
        String patientId = "patient-200";
        medicalStaffService.save(createSampleCreateRequest(doctorId));
        when(patientClient.getPatient(patientId)).thenReturn(null);

        medicalStaffService.assignPatient(doctorId, patientId);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalStaffService.assignPatient(doctorId, patientId)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Patient is already assigned", exception.getReason());
    }

    @Test
    void shouldUnassignPatient() {
        String doctorId = "doc-6";
        String patientId = "patient-300";
        medicalStaffService.save(createSampleCreateRequest(doctorId));
        when(patientClient.getPatient(patientId)).thenReturn(null);

        medicalStaffService.assignPatient(doctorId, patientId);
        assertTrue(medicalStaffService.getPatientsIds(doctorId).contains(patientId));

        medicalStaffService.unassignPatient(doctorId, patientId);

        assertFalse(medicalStaffService.getPatientsIds(doctorId).contains(patientId));
    }

    @Test
    void shouldGetAllMedicalStaff() {
        medicalStaffService.save(createSampleCreateRequest("doc-get-1", 1));
        medicalStaffService.save(createSampleCreateRequest("doc-get-2", 2));

        List<MedicalStaffResponse> response = medicalStaffService.getAll();

        assertTrue(response.size() >= 2);
        assertTrue(response.stream().anyMatch(d -> d.id().equals("doc-get-1")));
        assertTrue(response.stream().anyMatch(d -> d.id().equals("doc-get-2")));
    }

    @Test
    void shouldGetMedicalStaffById() {
        medicalStaffService.save(createSampleCreateRequest("doc-get-3", 3));

        MedicalStaffResponse response = medicalStaffService.getById("doc-get-3");

        assertNotNull(response);
        assertEquals("doc-get-3", response.id());
        assertEquals("Jan", response.firstName());
    }

    @Test
    void shouldThrowExceptionWhenGetByIdNotFound() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalStaffService.getById("non-existent-id")
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldGetDoctorsIdsByPatientId() {
        String doctor1Id = "doc-get-4";
        String doctor2Id = "doc-get-5";
        String patientId = "patient-shared-1";

        medicalStaffService.save(createSampleCreateRequest(doctor1Id, 4));
        medicalStaffService.save(createSampleCreateRequest(doctor2Id, 5));

        when(patientClient.getPatient(patientId)).thenReturn(null);

        medicalStaffService.assignPatient(doctor1Id, patientId);
        medicalStaffService.assignPatient(doctor2Id, patientId);

        List<String> doctorsIds = medicalStaffService.getDoctorsIdsByPatientId(patientId);

        assertEquals(2, doctorsIds.size());
        assertTrue(doctorsIds.contains(doctor1Id));
        assertTrue(doctorsIds.contains(doctor2Id));
    }

    @Test
    void shouldGetAllDoctorsEssentialData() {
        medicalStaffService.save(createSampleCreateRequest("doc-get-6", 6));
        medicalStaffService.save(createSampleCreateRequest("doc-get-7", 7));

        List<MedicalStaffEssentialResponse> response = medicalStaffService.getAllDoctorsEssentialData();

        assertFalse(response.isEmpty());
        assertTrue(response.stream().anyMatch(d -> d.id().equals("doc-get-6")));
    }

    @Test
    void shouldGetDoctorEssentialDataById() {
        medicalStaffService.save(createSampleCreateRequest("doc-get-8", 8));

        MedicalStaffEssentialResponse response = medicalStaffService.getDoctorEssentialDataById("doc-get-8");

        assertNotNull(response);
        assertEquals("doc-get-8", response.id());
    }

    @Test
    void shouldGetDoctorsAssignedToPatient() {
        String doctorId = "doc-get-9";
        String otherDoctorId = "doc-get-10";
        String targetPatientId = "patient-target-1";

        medicalStaffService.save(createSampleCreateRequest(doctorId, 9));
        medicalStaffService.save(createSampleCreateRequest(otherDoctorId, 10));

        when(patientClient.getPatient(anyString())).thenReturn(null);

        medicalStaffService.assignPatient(doctorId, targetPatientId);
        medicalStaffService.assignPatient(otherDoctorId, "other-patient-999");

        List<MedicalStaffEssentialResponse> responses = medicalStaffService.getDoctorsAssignedToPatient(targetPatientId);

        assertEquals(1, responses.size());
        assertEquals(doctorId, responses.getFirst().id());
    }

    private MedicalStaffCreateRequest createSampleCreateRequest(String id, int uniqueSuffix) {
        SpecializationRequest spec = new SpecializationRequest(
                "Cardiology",
                LocalDate.of(2015, 5, 20),
                "123456" + uniqueSuffix
        );

        return new MedicalStaffCreateRequest(
                id,
                "Jan",
                "Kowalski",
                "12345678" + uniqueSuffix,
                "123456" + uniqueSuffix,
                List.of(spec),
                "securePassword123"
        );
    }


    private MedicalStaffCreateRequest createSampleCreateRequest(String id) {
        SpecializationRequest spec = new SpecializationRequest(
                "Cardiology",
                LocalDate.of(2015, 5, 20),
                "1234567"
        );

        return new MedicalStaffCreateRequest(
                id,
                "Jan",
                "Kowalski",
                "123456789",
                "1234567",
                List.of(spec),
                "securePassword123"
        );
    }
}