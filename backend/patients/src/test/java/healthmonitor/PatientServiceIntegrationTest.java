package healthmonitor;

import healthmonitor.exception.exceptions.DuplicateResourceException;
import healthmonitor.exception.exceptions.PatientNotFoundException;
import healthmonitor.model.dto.PatientDto;
import healthmonitor.repository.PatientRepository;
import healthmonitor.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
public class PatientServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
    }

    private PatientDto createSampleDto(String pesel) {
        PatientDto dto = new PatientDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setFirstName("Jan");
        dto.setLastName("Kowalski");
        dto.setEmail(pesel + "@example.com");
        dto.setPesel(pesel != null ? pesel : "11111111111");
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dto.setPhoneNumber("123456789");
        dto.setAddress("Testowa 1, Warszawa");
        return dto;
    }

    @Test
    void shouldSaveAndRetrievePatient() {
        PatientDto dto = createSampleDto("12345678901");
        PatientDto saved = patientService.save(dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo(dto.getEmail());
    }

    @Test
    void shouldUpdatePatientData() {
        PatientDto saved = patientService.save(createSampleDto("12345678901"));

        PatientDto update = createSampleDto("12345678901");
        update.setLastName("Nowak");

        PatientDto updated = patientService.updatePatient(saved.getId(), update);

        assertThat(updated.getLastName()).isEqualTo("Nowak");
    }

    @Test
    void shouldThrowExceptionWhenPatientNotFound() {
        assertThrows(PatientNotFoundException.class, () -> patientService.getPatientById("non-existent-id"));
    }

    @Test
    void shouldThrowExceptionWhenPeselIsDuplicatedOnUpdate() {
        patientService.save(createSampleDto("11111111111"));
        PatientDto second = patientService.save(createSampleDto("22222222222"));

        PatientDto update = createSampleDto("11111111111");

        assertThrows(DuplicateResourceException.class, () -> patientService.updatePatient(second.getId(), update));
    }

    @Test
    void shouldGetAllPatients() {
        patientService.save(createSampleDto("11111111111"));
        patientService.save(createSampleDto("22222222222"));

        List<PatientDto> allPatients = patientService.getAllPatients();

        assertThat(allPatients.size()).isEqualTo(2);
    }

    @Test
    void shouldNotUpdateIfDataIsTheSame() {
        PatientDto dto = createSampleDto("11111111111");
        PatientDto saved = patientService.save(dto);

        PatientDto updated = patientService.updatePatient(saved.getId(), dto);

        assertThat(updated.getLastName()).isEqualTo(saved.getLastName());
        assertThat(patientRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionOnInvalidIdGet() {
        assertThrows(PatientNotFoundException.class, () -> patientService.getPatientById("invalid-uuid"));
    }
}
