package healthmonitor;

import healthmonitor.client.AuthClient;
import healthmonitor.client.PatientClient;
import healthmonitor.medicalStaff.mapper.MedicalStaffMapper;
import healthmonitor.medicalStaff.model.MedicalStaff;
import healthmonitor.medicalStaff.model.PatientAssignment;
import healthmonitor.medicalStaff.payload.request.MedicalStaffCreateRequest;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.payload.response.MedicalStaffEssentialResponse;
import healthmonitor.medicalStaff.payload.response.MedicalStaffResponse;
import healthmonitor.medicalStaff.repository.MedicalStaffRepository;
import healthmonitor.medicalStaff.service.MedicalStaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalStaffServiceImplTest {

    @Mock
    private MedicalStaffRepository medicalStaffRepository;

    @Mock
    private MedicalStaffMapper medicalStaffMapper;

    @Mock
    private PatientClient patientClient;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private MedicalStaffServiceImpl medicalStaffService;

    private MedicalStaff medicalStaff;

    @BeforeEach
    void setUp() {
        medicalStaff = new MedicalStaff();
        medicalStaff.setId("doc-123");
        medicalStaff.setFirstName("Jan");
        medicalStaff.setLastName("Kowalski");
        medicalStaff.setPatientAssignments(new ArrayList<>());
    }

    @Test
    void getAll_ShouldReturnMappedResponses() {
        // given
        when(medicalStaffRepository.findAll()).thenReturn(List.of(medicalStaff));
        when(medicalStaffMapper.toResponse(medicalStaff)).thenReturn(mock(MedicalStaffResponse.class));

        // when
        List<MedicalStaffResponse> result = medicalStaffService.getAll();

        // then
        assertEquals(1, result.size());
        verify(medicalStaffRepository, times(1)).findAll();
        verify(medicalStaffMapper, times(1)).toResponse(medicalStaff);
    }

    @Test
    void getById_ShouldReturnResponse_WhenStaffExists() {
        // given
        when(medicalStaffRepository.findWithSpecializationById("doc-123")).thenReturn(Optional.of(medicalStaff));
        MedicalStaffResponse responseMock = mock(MedicalStaffResponse.class);
        when(medicalStaffMapper.toResponse(medicalStaff)).thenReturn(responseMock);

        // when
        MedicalStaffResponse result = medicalStaffService.getById("doc-123");

        // then
        assertNotNull(result);
        verify(medicalStaffRepository, times(1)).findWithSpecializationById("doc-123");
    }

    @Test
    void getById_ShouldThrowResponseStatusException_WhenStaffDoesNotExist() {
        // given
        when(medicalStaffRepository.findWithSpecializationById("doc-999")).thenReturn(Optional.empty());

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> medicalStaffService.getById("doc-999"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Medical staff not found", exception.getReason());
    }

    @Test
    void save_ShouldCreateStaffAndUpdatePasswordInAuthService() {
        // given
        MedicalStaffCreateRequest request = new MedicalStaffCreateRequest(
                "doc-123", "Jan", "Kowalski", "123456789", "1234567", List.of(), "secretPass"
        );

        when(medicalStaffMapper.toEntity(request)).thenReturn(medicalStaff);
        when(medicalStaffRepository.save(medicalStaff)).thenReturn(medicalStaff);
        when(medicalStaffMapper.toResponse(medicalStaff)).thenReturn(mock(MedicalStaffResponse.class));

        // when
        medicalStaffService.save(request);

        // then
        // Weryfikacja integracji z AuthClient
        verify(authClient, times(1)).updatePassword("doc-123", "secretPass");
        verify(medicalStaffRepository, times(1)).save(medicalStaff);
    }

    @Test
    void update_ShouldUpdateEntityAndCallAuthClient_WhenPasswordIsProvided() {
        // given
        MedicalStaffRequest request = new MedicalStaffRequest(
                "Adam", "Nowak", "987654321", "7654321", List.of(), "newPass"
        );

        when(medicalStaffRepository.findWithSpecializationById("doc-123")).thenReturn(Optional.of(medicalStaff));

        // when
        medicalStaffService.update("doc-123", request);

        // then
        verify(authClient, times(1)).updatePassword("doc-123", "newPass");
        verify(medicalStaffMapper, times(1)).updateEntity(medicalStaff, request);
    }

    @Test
    void update_ShouldNotCallAuthClient_WhenPasswordIsBlank() {
        // given
        MedicalStaffRequest request = new MedicalStaffRequest(
                "Adam", "Nowak", "987654321", "7654321", List.of(), "   " // puste hasło
        );

        when(medicalStaffRepository.findWithSpecializationById("doc-123")).thenReturn(Optional.of(medicalStaff));

        // when
        medicalStaffService.update("doc-123", request);

        // then
        // Weryfikujemy, że metoda updatePassword w ogóle NIE została wywołana
        verify(authClient, never()).updatePassword(anyString(), anyString());
        verify(medicalStaffMapper, times(1)).updateEntity(medicalStaff, request);
    }

    @Test
    void assignPatient_ShouldAddAssignmentAndSave_WhenNotAlreadyAssigned() {
        // given
        String patientId = "pat-1";
        when(medicalStaffRepository.findWithSpecializationById("doc-123")).thenReturn(Optional.of(medicalStaff));

        // when
        medicalStaffService.assignPatient("doc-123", patientId);

        // then
        verify(patientClient, times(1)).getPatient(patientId); // weryfikacja czy sprawdzano istnienie pacjenta
        assertEquals(1, medicalStaff.getPatientAssignments().size());
        assertEquals("pat-1", medicalStaff.getPatientAssignments().get(0).getPatientId());
        verify(medicalStaffRepository, times(1)).save(medicalStaff);
    }

    @Test
    void assignPatient_ShouldThrowResponseStatusException_WhenPatientAlreadyAssigned() {
        // given
        String patientId = "pat-1";
        PatientAssignment existingAssignment = new PatientAssignment();
        existingAssignment.setPatientId(patientId);
        medicalStaff.addPatientAssignment(existingAssignment);

        when(medicalStaffRepository.findWithSpecializationById("doc-123")).thenReturn(Optional.of(medicalStaff));

        // when & then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> medicalStaffService.assignPatient("doc-123", patientId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Patient is already assigned", exception.getReason());

        // Zapis do bazy nie powinien się wykonać
        verify(medicalStaffRepository, never()).save(any());
    }

    @Test
    void unassignPatient_ShouldRemoveAssignment_WhenExists() {
        // given
        String patientId = "pat-1";
        PatientAssignment assignment = new PatientAssignment();
        assignment.setPatientId(patientId);
        assignment.setMedicalStaff(medicalStaff);
        medicalStaff.getPatientAssignments().add(assignment);

        when(medicalStaffRepository.findWithSpecializationById("doc-123")).thenReturn(Optional.of(medicalStaff));

        // when
        medicalStaffService.unassignPatient("doc-123", patientId);

        // then
        assertTrue(medicalStaff.getPatientAssignments().isEmpty());
    }

    @Test
    void getPatientsIds_ShouldReturnListOfIds() {
        // given
        PatientAssignment assignment1 = new PatientAssignment();
        assignment1.setPatientId("pat-1");
        PatientAssignment assignment2 = new PatientAssignment();
        assignment2.setPatientId("pat-2");

        medicalStaff.setPatientAssignments(List.of(assignment1, assignment2));

        when(medicalStaffRepository.findWithPatientAssignmentsById("doc-123")).thenReturn(Optional.of(medicalStaff));

        // when
        List<String> patientIds = medicalStaffService.getPatientsIds("doc-123");

        // then
        assertEquals(2, patientIds.size());
        assertTrue(patientIds.contains("pat-1"));
        assertTrue(patientIds.contains("pat-2"));
    }

    @Test
    void getAllDoctorsEssentialData_ShouldReturnMappedEssentialData() {
        // given
        when(medicalStaffRepository.findAll()).thenReturn(List.of(medicalStaff));
        when(medicalStaffMapper.toEssentialResponse(medicalStaff)).thenReturn(mock(MedicalStaffEssentialResponse.class));

        // when
        List<MedicalStaffEssentialResponse> result = medicalStaffService.getAllDoctorsEssentialData();

        // then
        assertEquals(1, result.size());
        verify(medicalStaffMapper, times(1)).toEssentialResponse(medicalStaff);
    }

    @Test
    void getDoctorsAssignedToPatient_ShouldReturnEssentialDataList() {
        // given
        String patientId = "pat-1";
        when(medicalStaffRepository.findByPatientAssignments_PatientId(patientId))
                .thenReturn(List.of(medicalStaff));

        MedicalStaffEssentialResponse essentialResponseMock = mock(MedicalStaffEssentialResponse.class);
        when(medicalStaffMapper.toEssentialResponse(medicalStaff))
                .thenReturn(essentialResponseMock);

        // when
        List<MedicalStaffEssentialResponse> result = medicalStaffService.getDoctorsAssignedToPatient(patientId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(medicalStaffRepository, times(1)).findByPatientAssignments_PatientId(patientId);
        verify(medicalStaffMapper, times(1)).toEssentialResponse(medicalStaff);
    }

    @Test
    void getDoctorEssentialDataById_ShouldReturnEssentialData_WhenStaffExists() {
        // given
        String doctorId = "doc-123";
        when(medicalStaffRepository.findWithSpecializationById(doctorId))
                .thenReturn(Optional.of(medicalStaff));

        MedicalStaffEssentialResponse essentialResponseMock = mock(MedicalStaffEssentialResponse.class);
        when(medicalStaffMapper.toEssentialResponse(medicalStaff))
                .thenReturn(essentialResponseMock);

        // when
        MedicalStaffEssentialResponse result = medicalStaffService.getDoctorEssentialDataById(doctorId);

        // then
        assertNotNull(result);
        verify(medicalStaffRepository, times(1)).findWithSpecializationById(doctorId);
        verify(medicalStaffMapper, times(1)).toEssentialResponse(medicalStaff);
    }

    @Test
    void delete_ShouldRemoveEntity_WhenStaffExists() {
        // given
        String doctorId = "doc-123";
        // Metoda getEntity() wywoływana wewnątrz delete() korzysta z findWithSpecializationById
        when(medicalStaffRepository.findWithSpecializationById(doctorId))
                .thenReturn(Optional.of(medicalStaff));

        // when
        assertDoesNotThrow(() -> medicalStaffService.delete(doctorId));

        // then
        verify(medicalStaffRepository, times(1)).findWithSpecializationById(doctorId);
        verify(medicalStaffRepository, times(1)).delete(medicalStaff);
    }
}