package healthmonitor;

import healthmonitor.exception.exceptions.DuplicateResourceException;
import healthmonitor.exception.exceptions.PatientNotFoundException;
import healthmonitor.mapper.PatientMapper;
import healthmonitor.model.Patient;
import healthmonitor.model.dto.PatientDto;
import healthmonitor.model.dto.PatientUpdateDto;
import healthmonitor.repository.PatientRepository;
import healthmonitor.service.StandardPatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StandardPatientService - Unit Tests")
class StandardPatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private StandardPatientService standardPatientService;

    private Patient patient;
    private PatientDto patientDto;
    private PatientUpdateDto patientUpdateDto;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId("1");
        patient.setEmail("john.doe@example.com");
        patient.setName("John");
        patient.setSurname("Doe");
        patient.setPesel("12345678901");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 15));
        patient.setPhoneNumber("+48123456789");
        patient.setAddress("123 Main Street");

        patientDto = new PatientDto();
        patientDto.setId("1");
        patientDto.setEmail("john.doe@example.com");
        patientDto.setName("John");
        patientDto.setSurname("Doe");
        patientDto.setPesel("12345678901");
        patientDto.setDateOfBirth(LocalDate.of(1990, 1, 15));
        patientDto.setPhoneNumber("+48123456789");
        patientDto.setAddress("123 Main Street");

        patientUpdateDto = new PatientUpdateDto();
        patientUpdateDto.setName("Johnny");
        patientUpdateDto.setSurname("Doe");
        patientUpdateDto.setPhoneNumber("+48987654321");
        patientUpdateDto.setAddress("456 Oak Avenue");
        patientUpdateDto.setDateOfBirth(LocalDate.of(1990, 1, 15));
    }

    @Test
    @DisplayName("Should create patient successfully")
    void testCreatePatientSuccess() {
        when(patientRepository.existsByEmail(patientDto.getEmail())).thenReturn(false);
        when(patientRepository.existsByPesel(patientDto.getPesel())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        PatientDto result = standardPatientService.createPatient(patientDto);

        assertNotNull(result);
        assertEquals(patientDto.getEmail(), result.getEmail());
        assertEquals(patientDto.getName(), result.getName());
        verify(patientRepository, times(1)).existsByEmail(patientDto.getEmail());
        verify(patientRepository, times(1)).existsByPesel(patientDto.getPesel());
        verify(patientRepository, times(1)).save(any(Patient.class));
        verify(patientMapper, times(1)).toDto(patient);
    }

    @Test
    @DisplayName("Should throw exception when user with this email exists")
    void testCreatePatientWithDuplicateEmail() {
        when(patientRepository.existsByEmail(patientDto.getEmail())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> standardPatientService.createPatient(patientDto));
        assertEquals("Patient with this e-mail already exists", exception.getMessage());
        verify(patientRepository, times(1)).existsByEmail(patientDto.getEmail());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should throw exception when user with this PESEL exists")
    void testCreatePatientWithDuplicatePesel() {
        when(patientRepository.existsByEmail(patientDto.getEmail())).thenReturn(false);
        when(patientRepository.existsByPesel(patientDto.getPesel())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> standardPatientService.createPatient(patientDto));
        assertEquals("Patient with this PESEL number already exists", exception.getMessage());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should find patient by ID successfully")
    void testGetPatientByIdSuccess() {
        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        PatientDto result = standardPatientService.getPatientById("1");

        assertNotNull(result);
        assertEquals(patientDto.getId(), result.getId());
        assertEquals(patientDto.getEmail(), result.getEmail());
        verify(patientRepository, times(1)).findById("1");
        verify(patientMapper, times(1)).toDto(patient);
    }

    @Test
    @DisplayName("Should throw exception when patient with given ID does not exist")
    void testGetPatientByIdNotFound() {
        when(patientRepository.findById("999")).thenReturn(Optional.empty());

        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class,
                () -> standardPatientService.getPatientById("999"));
        assertTrue(exception.getMessage().contains("Patient with this ID doesn't exists"));
        verify(patientRepository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Should get list of all patients successfully")
    void testGetAllPatients() {
        Patient patient2 = new Patient();
        patient2.setId("2");
        patient2.setEmail("jane.smith@example.com");
        patient2.setName("Jane");
        patient2.setSurname("Smith");

        PatientDto patientDto2 = new PatientDto();
        patientDto2.setId("2");
        patientDto2.setEmail("jane.smith@example.com");
        patientDto2.setName("Jane");
        patientDto2.setSurname("Smith");

        when(patientRepository.findAll()).thenReturn(Arrays.asList(patient, patient2));
        when(patientMapper.toDto(patient)).thenReturn(patientDto);
        when(patientMapper.toDto(patient2)).thenReturn(patientDto2);

        List<PatientDto> result = standardPatientService.getAllPatients();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(patientDto.getEmail(), result.get(0).getEmail());
        assertEquals(patientDto2.getEmail(), result.get(1).getEmail());
        verify(patientRepository, times(1)).findAll();
        verify(patientMapper, times(2)).toDto(any(Patient.class));
    }

    @Test
    @DisplayName("Should return empty list when there are no patients")
    void testGetAllPatientsEmpty() {
        when(patientRepository.findAll()).thenReturn(Arrays.asList());

        List<PatientDto> result = standardPatientService.getAllPatients();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(patientRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update patient successfully")
    void testUpdatePatientSuccess() {
        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        PatientDto result = standardPatientService.updatePatient("1", patientUpdateDto);

        assertNotNull(result);
        assertEquals(patientDto.getId(), result.getId());
        verify(patientRepository, times(1)).findById("1");
        verify(patientRepository, times(1)).save(any(Patient.class));
        verify(patientMapper, times(1)).toDto(patient);
    }

    @Test
    @DisplayName("Should throw exception when user doesn't exist for update")
    void testUpdatePatientNotFound() {
        when(patientRepository.findById("999")).thenReturn(Optional.empty());

        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class,
                () -> standardPatientService.updatePatient("999", patientUpdateDto));
        assertTrue(exception.getMessage().contains("Patient with this ID doesn't exists: "));
        verify(patientRepository, times(1)).findById("999");
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should update only provided fields and ignore null values")
    void testUpdatePatientPartialUpdate() {
        PatientUpdateDto partialUpdate = new PatientUpdateDto();
        partialUpdate.setName("Johnny");

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        PatientDto result = standardPatientService.updatePatient("1", partialUpdate);

        assertNotNull(result);
        verify(patientRepository, times(1)).findById("1");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should update all fields when all fields are provided in update request")
    void testUpdatePatientAllFields() {
        LocalDate newDate = LocalDate.of(1995, 5, 20);
        PatientUpdateDto fullUpdate = new PatientUpdateDto();
        fullUpdate.setName("Jane");
        fullUpdate.setSurname("Smith");
        fullUpdate.setPhoneNumber("+48111222333");
        fullUpdate.setAddress("789 Elm Street");
        fullUpdate.setDateOfBirth(newDate);

        Patient updatedPatient = new Patient();
        updatedPatient.setId("1");
        updatedPatient.setEmail("john.doe@example.com");
        updatedPatient.setName("Jane");
        updatedPatient.setSurname("Smith");
        updatedPatient.setPhoneNumber("+48111222333");
        updatedPatient.setAddress("789 Elm Street");
        updatedPatient.setDateOfBirth(newDate);

        PatientDto updatedDto = new PatientDto();
        updatedDto.setId("1");
        updatedDto.setName("Jane");
        updatedDto.setSurname("Smith");

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(updatedPatient);
        when(patientMapper.toDto(updatedPatient)).thenReturn(updatedDto);

        PatientDto result = standardPatientService.updatePatient("1", fullUpdate);

        assertNotNull(result);
        verify(patientRepository, times(1)).findById("1");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }
}
