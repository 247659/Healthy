package healthmonitor;

import healthmonitor.exception.exceptions.DuplicateResourceException;
import healthmonitor.exception.exceptions.PatientNotFoundException;
import healthmonitor.mapper.PatientMapper;
import healthmonitor.model.Patient;
import healthmonitor.model.dto.PatientDto;
import healthmonitor.model.event.PatientThresholdEvent;
import healthmonitor.repository.PatientRepository;
import healthmonitor.service.StandardPatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandardPatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private StandardPatientService patientService;

    private Patient patient;
    private PatientDto patientDto;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId("1");
        patient.setFirstName("Jan");
        patient.setLastName("Kowalski");
        patient.setPesel("12345678901");

        patientDto = new PatientDto();
        patientDto.setFirstName("Jan");
        patientDto.setLastName("Kowalski");
        patientDto.setPesel("12345678901");
    }

    @Test
    void getPatientById_ShouldReturnPatientDto_WhenPatientExists() {
        // given
        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        // when
        PatientDto result = patientService.getPatientById("1");

        // then
        assertNotNull(result);
        assertEquals("Jan", result.getFirstName());
        verify(patientRepository, times(1)).findById("1");
        verify(patientMapper, times(1)).toDto(patient);
    }

    @Test
    void getPatientById_ShouldThrowPatientNotFoundException_WhenPatientDoesNotExist() {
        // given
        when(patientRepository.findById("1")).thenReturn(Optional.empty());

        // when & then
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class,
                () -> patientService.getPatientById("1"));
        assertEquals("Patient with this ID doesn't exists: 1", exception.getMessage());
        verify(patientRepository, times(1)).findById("1");
        verifyNoInteractions(patientMapper);
    }

    @Test
    void getAllPatients_ShouldReturnListOfPatientDtos() {
        // given
        when(patientRepository.findAll()).thenReturn(List.of(patient));
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        // when
        List<PatientDto> result = patientService.getAllPatients();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).getFirstName());
        verify(patientRepository, times(1)).findAll();
        verify(patientMapper, times(1)).toDto(patient);
    }

    @Test
    void save_ShouldSavePatientAndPublishEvent() {
        // given
        when(patientMapper.toEntity(patientDto)).thenReturn(patient);
        when(patientRepository.save(patient)).thenReturn(patient);
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        // when
        PatientDto result = patientService.save(patientDto);

        // then
        assertNotNull(result);
        verify(patientRepository, times(1)).save(patient);
        verify(patientMapper, times(1)).toEntity(patientDto);
        verify(patientMapper, times(1)).toDto(patient);

        // Weryfikacja publikacji zdarzenia
        ArgumentCaptor<PatientThresholdEvent> eventCaptor = ArgumentCaptor.forClass(PatientThresholdEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals("1", eventCaptor.getValue().patientId());
    }

    @Test
    void updatePatient_ShouldUpdateFieldsAndReturnDto_WhenValidRequest() {
        // given
        PatientDto updateRequest = new PatientDto();
        updateRequest.setFirstName("Adam");
        updateRequest.setPesel("10987654321");

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.existsByPesel("10987654321")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(updateRequest);

        // when
        PatientDto result = patientService.updatePatient("1", updateRequest);

        // then
        assertNotNull(result);
        assertEquals("Adam", patient.getFirstName()); // Upewniamy się, że encja została zaktualizowana
        assertEquals("10987654321", patient.getPesel());

        verify(patientRepository, times(1)).findById("1");
        verify(patientRepository, times(1)).existsByPesel("10987654321");
        verify(patientRepository, times(1)).save(patient);
    }

    @Test
    void updatePatient_ShouldThrowPatientNotFoundException_WhenPatientDoesNotExist() {
        // given
        when(patientRepository.findById("1")).thenReturn(Optional.empty());

        // when & then
        assertThrows(PatientNotFoundException.class,
                () -> patientService.updatePatient("1", patientDto));

        verify(patientRepository, times(1)).findById("1");
        verify(patientRepository, never()).save(any());
    }

    @Test
    void updatePatient_ShouldThrowDuplicateResourceException_WhenNewPeselAlreadyExists() {
        // given
        PatientDto updateRequest = new PatientDto();
        updateRequest.setPesel("99999999999"); // Inny niż obecny

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.existsByPesel("99999999999")).thenReturn(true);

        // when & then
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> patientService.updatePatient("1", updateRequest));
        assertEquals("Patient with this PESEL number already exists", exception.getMessage());

        verify(patientRepository, times(1)).findById("1");
        verify(patientRepository, times(1)).existsByPesel("99999999999");
        verify(patientRepository, never()).save(any());
    }

    @Test
    void updatePatient_ShouldNotCheckPeselExistence_WhenPeselIsTheSame() {
        // given
        PatientDto updateRequest = new PatientDto();
        updateRequest.setPesel("12345678901"); // Taki sam jak ma pacjent (patient.getPesel())
        updateRequest.setFirstName("Nowe Imie");

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(updateRequest);

        // when
        patientService.updatePatient("1", updateRequest);

        // then
        verify(patientRepository, never()).existsByPesel(anyString());
        verify(patientRepository, times(1)).save(patient);
    }

    @Test
    void updatePatient_ShouldUpdateAllFields_WhenAllFieldsAreDifferentAndNotNull() {
        // given
        PatientDto updateRequest = new PatientDto();
        updateRequest.setPesel("98765432109"); // Nowy PESEL
        updateRequest.setFirstName("NoweImie");
        updateRequest.setLastName("NoweNazwisko");
        updateRequest.setPhoneNumber("987654321");
        updateRequest.setAddress("Nowy Adres 123");
        updateRequest.setDateOfBirth(LocalDate.of(1995, 5, 5));

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.existsByPesel("98765432109")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(updateRequest);

        // when
        patientService.updatePatient("1", updateRequest);

        // then
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient savedPatient = patientCaptor.getValue();

        // Sprawdzamy, czy wszystkie instrukcje 'if' zadziałały i zaktualizowały encję
        assertEquals("98765432109", savedPatient.getPesel());
        assertEquals("NoweImie", savedPatient.getFirstName());
        assertEquals("NoweNazwisko", savedPatient.getLastName());
        assertEquals("987654321", savedPatient.getPhoneNumber());
        assertEquals("Nowy Adres 123", savedPatient.getAddress());
        assertEquals(LocalDate.of(1995, 5, 5), savedPatient.getDateOfBirth());
    }

    @Test
    void updatePatient_ShouldNotUpdateAnyField_WhenAllFieldsInRequestAreNull() {
        // given
        // Ustawiamy w encji dane początkowe, żeby upewnić się, że nie zostaną nadpisane nullem
        patient.setLastName("Kowalski");
        patient.setPhoneNumber("123123123");
        patient.setAddress("Stary Adres");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // Pusty DTO (wszystkie pola to null)
        PatientDto emptyUpdateRequest = new PatientDto();

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(emptyUpdateRequest);

        // when
        patientService.updatePatient("1", emptyUpdateRequest);

        // then
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient savedPatient = patientCaptor.getValue();

        // Żaden 'if' nie powinien wejść, więc dane encji muszą pozostać niezmienione
        assertEquals("12345678901", savedPatient.getPesel());
        assertEquals("Jan", savedPatient.getFirstName());
        assertEquals("Kowalski", savedPatient.getLastName());
        assertEquals("123123123", savedPatient.getPhoneNumber());
        assertEquals("Stary Adres", savedPatient.getAddress());
        assertEquals(LocalDate.of(1990, 1, 1), savedPatient.getDateOfBirth());

        // Nie powinno sprawdzać w bazie istnienia nulla jako PESELu
        verify(patientRepository, never()).existsByPesel(any());
    }

    @Test
    void updatePatient_ShouldNotUpdateAnyField_WhenAllFieldsAreIdentical() {
        // given
        patient.setLastName("Kowalski");
        patient.setPhoneNumber("123123123");
        patient.setAddress("Stary Adres");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // Obiekt żądania posiada DOKŁADNIE takie same dane jak obecna encja
        PatientDto identicalUpdateRequest = new PatientDto();
        identicalUpdateRequest.setPesel("12345678901");
        identicalUpdateRequest.setFirstName("Jan");
        identicalUpdateRequest.setLastName("Kowalski");
        identicalUpdateRequest.setPhoneNumber("123123123");
        identicalUpdateRequest.setAddress("Stary Adres");
        identicalUpdateRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(identicalUpdateRequest);

        // when
        patientService.updatePatient("1", identicalUpdateRequest);

        // then
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient savedPatient = patientCaptor.getValue();

        assertEquals("12345678901", savedPatient.getPesel());
        assertEquals("Jan", savedPatient.getFirstName());
        assertEquals("Kowalski", savedPatient.getLastName());
        assertEquals("123123123", savedPatient.getPhoneNumber());
        assertEquals("Stary Adres", savedPatient.getAddress());
        assertEquals(LocalDate.of(1990, 1, 1), savedPatient.getDateOfBirth());

        // Nie szukamy w bazie konfliktów z własnym numerem PESEL
        verify(patientRepository, never()).existsByPesel(any());
    }
}
