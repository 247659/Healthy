package healthmonitor.service;

import healthmonitor.exception.exceptions.DuplicateResourceException;
import healthmonitor.exception.exceptions.PatientNotFoundException;
import healthmonitor.mapper.PatientMapper;
import healthmonitor.model.Patient;
import healthmonitor.model.dto.PatientDto;
import healthmonitor.model.dto.PatientRegistrationDto;
import healthmonitor.repository.PatientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandardPatientService implements PatientService {

    private final KeycloakUserService keycloakUserService;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Transactional
    @Override
    public PatientDto registerPatient(PatientRegistrationDto dto) {
        if (patientRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Patient with this e-mail already exists");
        }

        String userId = keycloakUserService.createUserInKeycloak(
                dto.getEmail(), dto.getPassword(), dto.getFirstName(), dto.getLastName());

        Patient patient = new Patient();
        patient.setId(userId);
        patient.setEmail(dto.getEmail());
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());

        Patient savedPatient = patientRepository.save(patient);
        return patientMapper.toDto(savedPatient);
    }

    @Override
    public PatientDto getPatientById(String id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient with this ID doesn't exists: " + id));
        return patientMapper.toDto(patient);
    }

    @Override
    public List<PatientDto> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(patientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public PatientDto updatePatient(String id, PatientDto updateRequest) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient with this ID doesn't exists: " + id));

        if (updateRequest.getPesel() != null && patientRepository.existsByPesel(updateRequest.getPesel())) {
            throw new DuplicateResourceException("Patient with this PESEL number already exists");
        } else {
            patient.setPesel(updateRequest.getPesel());
        }

        if (!patient.getFirstName().equals(updateRequest.getFirstName())) {
            patient.setFirstName(updateRequest.getFirstName());
        }
        if (!patient.getLastName().equals(updateRequest.getLastName())) {
            patient.setLastName(updateRequest.getLastName());
        }
        if (!patient.getPhoneNumber().equals(updateRequest.getPhoneNumber())) {
            patient.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (!patient.getAddress().equals(updateRequest.getAddress())) {
            patient.setAddress(updateRequest.getAddress());
        }
        if (!patient.getDateOfBirth().equals(updateRequest.getDateOfBirth())) {
            patient.setDateOfBirth(updateRequest.getDateOfBirth());
        }

        Patient updatedPatient = patientRepository.save(patient);
        return patientMapper.toDto(updatedPatient);
    }

}
